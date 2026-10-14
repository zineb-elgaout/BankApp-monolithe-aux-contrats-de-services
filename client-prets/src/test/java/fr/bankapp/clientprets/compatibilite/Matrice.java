package fr.bankapp.clientprets.compatibilite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.bankapp.contrat.ContratOpenApi;
import fr.bankapp.contrat.ErreurMessage;

import java.nio.file.Path;
import java.util.List;

/**
 * Outillage fourni pour la matrice de compatibilité.
 * Un « service » est représenté par son contrat : ce qu'il accepte (le schéma de la demande)
 * et ce qu'il répond (l'exemple de sa réponse 200). Aucun serveur n'est nécessaire.
 */
final class Matrice {

    /** Ce qu'envoie un client écrit pour la version 1.0 : il ne connaît pas le revenu mensuel. */
    static final String ANCIENNE_DEMANDE = """
            { "idClient": "C-1001", "montant": "20000.00", "dureeMois": 24 }""";

    /** Ce qu'envoie un client écrit pour la version 1.1 : il transmet le revenu mensuel. */
    static final String NOUVELLE_DEMANDE = """
            { "idClient": "C-1001", "montant": "20000.00", "dureeMois": 24, "revenuMensuel": "3500.00" }""";

    private static final ObjectMapper JSON = new ObjectMapper();

    private Matrice() {
    }

    /** Charge un contrat du dossier contrats, par exemple "risque.yaml" ou "versions/risque-1.0.yaml". */
    static ContratOpenApi contrat(String fichier) {
        return ContratOpenApi.charger(Path.of("../contrats").resolve(fichier));
    }

    /** Envoie la demande au service : retourne les erreurs qu'il renverrait (liste vide : demande acceptée). */
    static List<ErreurMessage> envoyer(ContratOpenApi service, String demande) {
        try {
            JsonNode message = JSON.readTree(demande);
            return service.valider(service.composant("DemandePretDto"), message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Demande JSON mal formée : " + demande, e);
        }
    }

    /** Fait lire la réponse de succès du service par le lecteur du client (Evaluation, EvaluationV11...). */
    static <T> T recevoir(ContratOpenApi service, Class<T> lecteur) {
        ContratOpenApi.Operation operation = service.trouver("POST", "/risque/evaluerRisque")
                .orElseThrow(() -> new IllegalStateException("Le contrat ne déclare pas POST /risque/evaluerRisque"));
        JsonNode exemple = service.reponse(operation, 200)
                .orElseThrow(() -> new IllegalStateException("Le contrat ne déclare pas de réponse 200"))
                .exemple();
        try {
            return JSON.treeToValue(exemple, lecteur);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Le client ne sait pas lire la réponse : " + e.getOriginalMessage(), e);
        }
    }
}
