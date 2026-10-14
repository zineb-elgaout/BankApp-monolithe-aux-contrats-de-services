package fr.bankapp.contrat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service simulé généré à partir d'un contrat OpenAPI, sans une ligne de code métier.
 *
 * Pour chaque requête, le simulateur :
 *   1. cherche l'opération dans le contrat (sinon : OPERATION_INCONNUE) ;
 *   2. valide le corps de la requête contre le schéma du contrat (sinon : MESSAGE_INVALIDE) ;
 *   3. renvoie l'exemple de la réponse prévue par le contrat.
 * L'en-tête « Prefer: code=404 » demande la réponse 404 du contrat, si le contrat en prévoit une.
 *
 * Lancement : mvn -q -pl outils-contrat exec:java "-Dexec.args=contrats/risque.yaml 4010"
 */
public class SimulateurContrat {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern PREFER_CODE = Pattern.compile("code=(\\d{3})");

    private final ContratOpenApi contrat;

    public SimulateurContrat(ContratOpenApi contrat) {
        this.contrat = contrat;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage : SimulateurContrat <contrat.yaml> [port]");
            return;
        }
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 4010;
        demarrer(Path.of(args[0]), port);
        System.out.println("Appuyez sur Ctrl+C pour arrêter le simulateur.");
    }

    /** Démarre le simulateur. Avec le port 0, un port libre est choisi (utile pour les tests). */
    public static HttpServer demarrer(Path fichierContrat, int port) throws IOException {
        ContratOpenApi contrat = ContratOpenApi.charger(fichierContrat);
        SimulateurContrat simulateur = new SimulateurContrat(contrat);
        HttpServer serveur = HttpServer.create(new InetSocketAddress(port), 0);
        serveur.createContext("/", simulateur::traiter);
        serveur.start();
        System.out.println("Simulateur du contrat « " + contrat.titre() + " », version " + contrat.version()
                + ", sur http://localhost:" + serveur.getAddress().getPort());
        for (ContratOpenApi.Operation operation : contrat.operations()) {
            System.out.println("  " + operation.libelle());
        }
        simulateur.verifierLesExemples();
        return serveur;
    }

    /** Un contrat dont les exemples violent les schémas ment à ses consommateurs : on le signale. */
    void verifierLesExemples() {
        for (ContratOpenApi.Operation operation : contrat.operations()) {
            for (ContratOpenApi.Reponse reponse : contrat.reponses(operation)) {
                if (reponse.exemple() == null || reponse.schema() == null) {
                    continue;
                }
                for (ErreurMessage erreur : contrat.valider(reponse.schema(), reponse.exemple())) {
                    System.out.println("  ATTENTION : l'exemple de la réponse " + reponse.code() + " de "
                            + operation.operationId() + " ne respecte pas son schéma (" + erreur.champ() + " : "
                            + erreur.regle() + ")");
                }
            }
        }
    }

    void traiter(HttpExchange echange) throws IOException {
        String methode = echange.getRequestMethod();
        String chemin = echange.getRequestURI().getPath();
        Optional<ContratOpenApi.Operation> trouvee = contrat.trouver(methode, chemin);
        if (trouvee.isEmpty()) {
            repondre(echange, 404, erreur("OPERATION_INCONNUE", "Le contrat ne déclare aucune opération "
                    + methode + " " + chemin + ". Opérations déclarées : " + declarees()), "aucune opération");
            return;
        }
        ContratOpenApi.Operation operation = trouvee.get();

        JsonNode schema = contrat.schemaDeLaDemande(operation);
        if (schema != null) {
            String corps = new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode message;
            try {
                message = JSON.readTree(corps.isBlank() ? "null" : corps);
            } catch (JsonProcessingException e) {
                repondre(echange, 400, JSON.writeValueAsString(new ErreurMessage(ErreurMessage.MESSAGE_INVALIDE,
                        "(message)", "JSON bien formé", corps)), operation.operationId());
                return;
            }
            List<ErreurMessage> erreurs = contrat.valider(schema, message);
            if (!erreurs.isEmpty()) {
                erreurs.sort(Comparator.comparing(ErreurMessage::champ));
                repondre(echange, 400, JSON.writeValueAsString(erreurs.get(0)), operation.operationId());
                return;
            }
        }

        int codeDemande = codeDemande(echange);
        Optional<ContratOpenApi.Reponse> reponse = codeDemande > 0
                ? contrat.reponse(operation, codeDemande)
                : contrat.reponses(operation).stream().filter(r -> r.code() < 300)
                        .min(Comparator.comparingInt(ContratOpenApi.Reponse::code));
        if (reponse.isEmpty()) {
            repondre(echange, 501, erreur("REPONSE_NON_PREVUE", "Le contrat ne prévoit pas de réponse "
                    + (codeDemande > 0 ? codeDemande : "de succès") + " pour l'opération "
                    + operation.operationId()), operation.operationId());
            return;
        }
        if (reponse.get().exemple() == null) {
            repondre(echange, 501, erreur("EXEMPLE_MANQUANT", "La réponse " + reponse.get().code() + " de "
                    + operation.operationId() + " n'a pas d'exemple : le simulateur ne sait pas quoi répondre"),
                    operation.operationId());
            return;
        }
        repondre(echange, reponse.get().code(), JSON.writeValueAsString(reponse.get().exemple()),
                operation.operationId());
    }

    private int codeDemande(HttpExchange echange) {
        String prefer = echange.getRequestHeaders().getFirst("Prefer");
        if (prefer == null) {
            return 0;
        }
        Matcher code = PREFER_CODE.matcher(prefer);
        return code.find() ? Integer.parseInt(code.group(1)) : 0;
    }

    private String declarees() {
        return String.join(", ", contrat.operations().stream()
                .map(o -> o.methode() + " " + o.chemin()).toList());
    }

    private static String erreur(String code, String message) throws JsonProcessingException {
        ObjectNode corps = JSON.createObjectNode();
        corps.put("code", code);
        corps.put("message", message);
        return JSON.writeValueAsString(corps);
    }

    private static void repondre(HttpExchange echange, int code, String json, String operation) throws IOException {
        System.out.println("[simulateur] " + echange.getRequestMethod() + " " + echange.getRequestURI().getPath()
                + " -> " + code + " (" + operation + ")");
        byte[] corps = json.getBytes(StandardCharsets.UTF_8);
        echange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        echange.sendResponseHeaders(code, corps.length);
        try (OutputStream sortie = echange.getResponseBody()) {
            sortie.write(corps);
        }
    }
}
