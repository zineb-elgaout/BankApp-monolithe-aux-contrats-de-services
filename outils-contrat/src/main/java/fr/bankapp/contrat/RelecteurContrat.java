package fr.bankapp.contrat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Relecture automatique des contrats : les règles communes de la banque, appliquées à chaque contrat.
 * Les règles et leurs paramètres sont dans contrats/regles-contrat.yaml.
 *
 * Lancement : mvn -q -pl outils-contrat exec:java "-Dexec.mainClass=fr.bankapp.contrat.RelecteurContrat"
 *             "-Dexec.args=contrats/risque.yaml contrats/notification.yaml"
 */
public class RelecteurContrat {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    /** Un problème trouvé dans un contrat, localisé à la ligne près. */
    public record Probleme(String fichier, int ligne, String regle, String message) {

        @Override
        public String toString() {
            return fichier + ":" + ligne + "  " + regle + "  " + message;
        }
    }

    private final JsonNode regles;

    public RelecteurContrat(JsonNode regles) {
        this.regles = regles.path("regles");
    }

    public static void main(String[] args) throws IOException {
        Path fichierRegles = Path.of(System.getProperty("bankapp.regles", "contrats/regles-contrat.yaml"));
        RelecteurContrat relecteur = new RelecteurContrat(YAML.readTree(fichierRegles.toFile()));
        List<Probleme> problemes = new ArrayList<>();
        for (String argument : args) {
            problemes.addAll(relecteur.relire(Path.of(argument)));
        }
        problemes.forEach(System.out::println);
        System.out.println(problemes.isEmpty()
                ? "Aucun problème : " + args.length + " contrat(s) conforme(s) aux règles de la banque."
                : problemes.size() + " problème(s) dans " + args.length + " contrat(s).");
    }

    public List<Probleme> relire(Path fichier) throws IOException {
        List<String> lignes = Files.readAllLines(fichier);
        ContratOpenApi contrat = ContratOpenApi.charger(fichier);
        List<Probleme> problemes = new ArrayList<>();
        String nom = fichier.toString().replace('\\', '/');

        if (active("fiche-de-service")) {
            JsonNode fiche = contrat.racine().path("info").path("x-fiche");
            for (JsonNode champ : regles.path("fiche-de-service").path("champs")) {
                JsonNode valeur = fiche.path(champ.asText());
                boolean vide = valeur.isMissingNode() || valeur.isNull()
                        || (valeur.isArray() ? valeur.isEmpty() : valeur.asText().isBlank())
                        || valeur.toString().contains("TODO");
                if (vide) {
                    problemes.add(new Probleme(nom, ligne(lignes, "x-fiche"), "fiche-de-service",
                            "La fiche de service ne renseigne pas « " + champ.asText() + " »."));
                }
            }
        }

        for (ContratOpenApi.Operation operation : contrat.operations()) {
            if (active("verbe-objet")) {
                verifierNom(operation.operationId(), lignes, nom, problemes);
            }
            if (active("erreurs-normalisees")) {
                String schemaErreur = regles.path("erreurs-normalisees").path("schema").asText("Erreur");
                Iterator<Map.Entry<String, JsonNode>> reponses = operation.noeud().path("responses").fields();
                while (reponses.hasNext()) {
                    Map.Entry<String, JsonNode> reponse = reponses.next();
                    if (reponse.getKey().startsWith("2")) {
                        continue;
                    }
                    String ref = contrat.resoudre(reponse.getValue()).path("content").path("application/json")
                            .path("schema").path("$ref").asText("");
                    if (!ref.endsWith("/" + schemaErreur)) {
                        problemes.add(new Probleme(nom, ligne(lignes, "'" + reponse.getKey() + "'", "\"" + reponse.getKey() + "\""),
                                "erreurs-normalisees", "La réponse " + reponse.getKey() + " de " + operation.operationId()
                                + " doit utiliser le schéma commun " + schemaErreur + "."));
                    }
                }
            }
        }

        if (active("montant-en-chaine")) {
            verifierMontants(contrat.racine().path("components").path("schemas"), "", lignes, nom, problemes);
        }

        if (active("pas-de-detail-interne")) {
            for (JsonNode motif : regles.path("pas-de-detail-interne").path("motifs")) {
                Pattern expression = Pattern.compile(motif.asText());
                for (int i = 0; i < lignes.size(); i++) {
                    String ligne = lignes.get(i).replaceFirst("\\s*#.*$", "");
                    Matcher trouve = expression.matcher(ligne);
                    if (trouve.find()) {
                        problemes.add(new Probleme(nom, i + 1, "pas-de-detail-interne",
                                "« " + trouve.group() + " » révèle un détail de réalisation (table, classe, technologie)."));
                    }
                }
            }
        }
        return problemes;
    }

    private void verifierNom(String operationId, List<String> lignes, String nom, List<Probleme> problemes) {
        JsonNode regle = regles.path("verbe-objet");
        int ligne = ligne(lignes, "operationId: " + operationId);
        for (JsonNode admis : regle.path("noms-admis")) {
            if (operationId.equals(admis.asText())) {
                return;
            }
        }
        for (JsonNode interdit : regle.path("verbes-interdits")) {
            if (operationId.startsWith(interdit.asText())) {
                problemes.add(new Probleme(nom, ligne, "verbe-objet", "L'opération « " + operationId
                        + " » commence par « " + interdit.asText() + " » : nommez-la par un verbe du métier et un objet."));
                return;
            }
        }
        boolean verbeConnu = false;
        for (JsonNode verbe : regle.path("verbes-metier")) {
            if (operationId.startsWith(verbe.asText()) && operationId.length() > verbe.asText().length()
                    && Character.isUpperCase(operationId.charAt(verbe.asText().length()))) {
                verbeConnu = true;
            }
        }
        if (!verbeConnu) {
            problemes.add(new Probleme(nom, ligne, "verbe-objet", "L'opération « " + operationId
                    + " » ne suit pas la forme verbe + objet (ex. evaluerRisque, suivreEnvoi)."));
        }
    }

    private void verifierMontants(JsonNode noeud, String chemin, List<String> lignes, String nom, List<Probleme> problemes) {
        Iterator<Map.Entry<String, JsonNode>> champs = noeud.fields();
        while (champs.hasNext()) {
            Map.Entry<String, JsonNode> champ = champs.next();
            String nomChamp = champ.getKey();
            JsonNode valeur = champ.getValue();
            boolean estMontant = false;
            for (JsonNode mot : regles.path("montant-en-chaine").path("champs")) {
                estMontant |= nomChamp.toLowerCase().contains(mot.asText());
            }
            if (estMontant && valeur.isObject() && valeur.has("type") && !valeur.path("type").asText().equals("string")) {
                problemes.add(new Probleme(nom, ligne(lignes, nomChamp + ":"), "montant-en-chaine", "Le champ « "
                        + nomChamp + " » est déclaré en " + valeur.path("type").asText()
                        + " : un montant voyage en chaîne décimale (\"150000.00\")."));
            }
            if (valeur.isContainerNode()) {
                verifierMontants(valeur, chemin + "/" + nomChamp, lignes, nom, problemes);
            }
        }
    }

    private boolean active(String regle) {
        return regles.path(regle).path("active").asBoolean(false);
    }

    private static int ligne(List<String> lignes, String... textes) {
        for (int i = 0; i < lignes.size(); i++) {
            for (String texte : textes) {
                if (lignes.get(i).contains(texte)) {
                    return i + 1;
                }
            }
        }
        return 1;
    }
}
