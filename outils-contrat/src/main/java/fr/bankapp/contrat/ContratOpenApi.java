package fr.bankapp.contrat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lecture d'un contrat OpenAPI 3 écrit en YAML.
 * Sous-ensemble utile au cours : chemins, opérations, corps de requête, réponses et leurs exemples,
 * schémas déclarés dans components/schemas.
 */
public class ContratOpenApi {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final List<String> METHODES = List.of("get", "post", "put", "patch", "delete");

    private final Path fichier;
    private final JsonNode racine;

    private ContratOpenApi(Path fichier, JsonNode racine) {
        this.fichier = fichier;
        this.racine = racine;
    }

    public static ContratOpenApi charger(Path fichier) {
        try {
            return new ContratOpenApi(fichier, YAML.readTree(fichier.toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException("Contrat illisible : " + fichier + " (" + e.getMessage() + ")", e);
        }
    }

    /** Une opération du contrat : une méthode HTTP sur un chemin. */
    public record Operation(String methode, String chemin, String operationId, JsonNode noeud) {

        public String libelle() {
            return methode + " " + chemin + " (" + operationId + ")";
        }
    }

    /** Une réponse prévue par le contrat, avec l'exemple qui servira de réponse simulée. */
    public record Reponse(int code, JsonNode schema, JsonNode exemple) {
    }

    public Path fichier() {
        return fichier;
    }

    public JsonNode racine() {
        return racine;
    }

    public String titre() {
        return racine.path("info").path("title").asText("(sans titre)");
    }

    public String version() {
        return racine.path("info").path("version").asText("(sans version)");
    }

    public List<Operation> operations() {
        List<Operation> operations = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> chemins = racine.path("paths").fields();
        while (chemins.hasNext()) {
            Map.Entry<String, JsonNode> chemin = chemins.next();
            for (String methode : METHODES) {
                JsonNode noeud = chemin.getValue().get(methode);
                if (noeud != null) {
                    operations.add(new Operation(methode.toUpperCase(), chemin.getKey(),
                            noeud.path("operationId").asText("(sans operationId)"), noeud));
                }
            }
        }
        return operations;
    }

    public Optional<Operation> trouver(String methode, String chemin) {
        return operations().stream()
                .filter(o -> o.methode().equalsIgnoreCase(methode) && o.chemin().equals(chemin))
                .findFirst();
    }

    /** Schéma JSON du corps de la requête, ou null si l'opération n'a pas de corps. */
    public JsonNode schemaDeLaDemande(Operation operation) {
        JsonNode schema = operation.noeud().path("requestBody").path("content").path("application/json").path("schema");
        return schema.isMissingNode() ? null : schema;
    }

    public List<Reponse> reponses(Operation operation) {
        List<Reponse> reponses = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> codes = operation.noeud().path("responses").fields();
        while (codes.hasNext()) {
            Map.Entry<String, JsonNode> entree = codes.next();
            if (!entree.getKey().matches("\\d{3}")) {
                continue;
            }
            JsonNode contenu = resoudre(entree.getValue()).path("content").path("application/json");
            JsonNode exemple = contenu.get("example");
            if (exemple == null && contenu.has("examples")) {
                exemple = contenu.get("examples").elements().next().path("value");
            }
            reponses.add(new Reponse(Integer.parseInt(entree.getKey()), contenu.get("schema"), exemple));
        }
        return reponses;
    }

    public Optional<Reponse> reponse(Operation operation, int code) {
        return reponses(operation).stream().filter(r -> r.code() == code).findFirst();
    }

    /** Valide un message JSON contre un schéma du contrat et retourne les violations, traduites. */
    public List<ErreurMessage> valider(JsonNode schemaContrat, JsonNode message) {
        List<ErreurMessage> erreurs = new ArrayList<>();
        for (ValidationMessage violation : schemaJson(schemaContrat).validate(message)) {
            erreurs.add(ErreurMessage.depuis(violation));
        }
        return erreurs;
    }

    /** Schéma déclaré sous components/schemas, par son nom. */
    public JsonNode composant(String nom) {
        JsonNode schema = racine.path("components").path("schemas").get(nom);
        if (schema == null) {
            throw new IllegalArgumentException("Le contrat ne déclare pas le schéma " + nom);
        }
        return schema;
    }

    /**
     * Construit un JSON Schema autonome : le schéma demandé, plus tous les schémas de components,
     * avec les références #/components/schemas/X réécrites en #/definitions/X.
     */
    JsonSchema schemaJson(JsonNode schemaContrat) {
        ObjectNode autonome = (ObjectNode) reecrire(schemaContrat.deepCopy());
        JsonNode composants = racine.path("components").path("schemas");
        if (!composants.isMissingNode()) {
            autonome.set("definitions", reecrire(composants.deepCopy()));
        }
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(autonome);
    }

    private JsonNode reecrire(JsonNode noeud) {
        if (noeud.isObject()) {
            ObjectNode objet = (ObjectNode) noeud;
            JsonNode ref = objet.get("$ref");
            if (ref != null && ref.isTextual()) {
                objet.put("$ref", ref.asText().replace("#/components/schemas/", "#/definitions/"));
            }
            objet.fields().forEachRemaining(champ -> reecrire(champ.getValue()));
        } else if (noeud.isArray()) {
            noeud.forEach(this::reecrire);
        }
        return noeud;
    }

    /** Suit une référence #/components/... si le noeud en est une. */
    JsonNode resoudre(JsonNode noeud) {
        JsonNode ref = noeud.get("$ref");
        if (ref == null) {
            return noeud;
        }
        JsonNode cible = racine;
        for (String morceau : ref.asText().replaceFirst("^#/", "").split("/")) {
            cible = cible.path(morceau);
        }
        return cible;
    }
}
