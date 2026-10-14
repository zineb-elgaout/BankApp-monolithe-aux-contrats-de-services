package fr.bankapp.clientprets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Le client de l'équipe Prêts. Il a été écrit à partir du contrat du service Risque (chapitre 1, slide 19),
 * avant que le service existe : il ne doit jamais être modifié pour s'adapter à un contrat mal écrit.
 *
 * Contrat attendu :
 *   POST /risque/evaluerRisque
 *   demande : { "idClient": "C-1001", "montant": "20000.00", "dureeMois": 24 }
 *   réponse : { "score": 72, "decision": "ACCEPTE" }
 *   erreurs : CLIENT_INCONNU (404), MONTANT_INVALIDE
 */
public class ClientPrets {

    static final String OPERATION = "/risque/evaluerRisque";

    private final String adresse;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper json = new ObjectMapper();

    public ClientPrets(String adresse) {
        this.adresse = adresse;
    }

    public Evaluation evaluerRisque(DemandePretDto demande) {
        return appeler(demande, null);
    }

    /** Même appel, en demandant au service simulé une réponse précise (en-tête Prefer). */
    Evaluation appeler(DemandePretDto demande, String prefer) {
        try {
            HttpRequest.Builder requete = HttpRequest.newBuilder(URI.create(adresse + OPERATION))
                    .timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(demande)));
            if (prefer != null) {
                requete.header("Prefer", prefer);
            }
            HttpResponse<String> reponse = http.send(requete.build(), HttpResponse.BodyHandlers.ofString());
            if (reponse.statusCode() != 200) {
                JsonNode erreur = json.readTree(reponse.body());
                throw new ErreurContrat(reponse.statusCode(), erreur.path("code").asText("?"), detail(erreur));
            }
            return json.readValue(reponse.body(), Evaluation.class);
        } catch (IOException e) {
            throw new ErreurContrat(0, "SERVICE_INDISPONIBLE", adresse + " (" + e.getMessage() + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErreurContrat(0, "SERVICE_INDISPONIBLE", adresse);
        }
    }

    /** Le détail lisible d'une erreur : son message, ou le champ, la règle et la valeur reçue. */
    private static String detail(JsonNode erreur) {
        if (erreur.has("message")) {
            return erreur.path("message").asText();
        }
        String detail = erreur.path("champ").asText("") + ", " + erreur.path("regle").asText("");
        return erreur.has("recu") ? detail + ", reçu " + erreur.get("recu") : detail;
    }

    /** Les deux cas que l'équipe Prêts doit pouvoir traiter : un client connu, un client inconnu. */
    public static void main(String[] args) {
        ClientPrets client = new ClientPrets(args.length > 0 ? args[0] : "http://localhost:4010");

        System.out.println("1. Évaluation du risque de C-1001 : 20000.00 sur 24 mois");
        try {
            Evaluation evaluation = client.evaluerRisque(new DemandePretDto("C-1001", new BigDecimal("20000.00"), 24));
            System.out.println("   OK : score " + evaluation.score() + ", décision " + evaluation.decision());
        } catch (ErreurContrat e) {
            System.out.println("   ÉCHEC : " + e.getMessage());
        }

        System.out.println("2. Client inconnu C-9999 : le contrat doit prévoir l'erreur CLIENT_INCONNU");
        try {
            client.appeler(new DemandePretDto("C-9999", new BigDecimal("20000.00"), 24), "code=404");
            System.out.println("   ÉCHEC : le service aurait dû refuser un client inconnu");
        } catch (ErreurContrat e) {
            System.out.println(e.code().equals("CLIENT_INCONNU")
                    ? "   OK : erreur CLIENT_INCONNU, prévue par le contrat"
                    : "   ÉCHEC : " + e.getMessage());
        }
    }
}
