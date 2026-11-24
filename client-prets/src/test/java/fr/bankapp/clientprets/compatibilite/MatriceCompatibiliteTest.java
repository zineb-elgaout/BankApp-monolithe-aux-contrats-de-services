package fr.bankapp.clientprets.compatibilite;

import fr.bankapp.clientprets.Evaluation;
import fr.bankapp.contrat.ContratOpenApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Matrice de compatibilité du contrat Risque, version 1.0 contre version 1.1.
 * Une ligne par client, une colonne par service : les quatre cases doivent passer.
 */
@Tag("compatibilite")
class MatriceCompatibiliteTest {

    static final ContratOpenApi SERVICE_1_0 = Matrice.contrat("versions/risque-1.0.yaml");
    static final ContratOpenApi SERVICE_1_1 = Matrice.contrat("risque.yaml");

    @Test
    @DisplayName("Ancien client, ancien service")
    void ancienClient_ancienService() {
        assertEquals(List.of(), Matrice.envoyer(SERVICE_1_0, Matrice.ANCIENNE_DEMANDE));
        Evaluation evaluation = Matrice.recevoir(SERVICE_1_0, Evaluation.class);
        assertNotNull(evaluation.decision());
    }

    @Test
    @DisplayName("Ancien client, nouveau service : rétrocompatibilité (backward)")
    void ancienClient_nouveauService() {
        assertEquals(List.of(), Matrice.envoyer(SERVICE_1_1, Matrice.ANCIENNE_DEMANDE));
        Evaluation evaluation = Matrice.recevoir(SERVICE_1_1, Evaluation.class);
        assertNotNull(evaluation.decision());
    }

    @Test
    @DisplayName("Nouveau client, ancien service : compatibilité anticipée (forward)")
    void nouveauClient_ancienService() {
        assertEquals(List.of(), Matrice.envoyer(SERVICE_1_0, Matrice.NOUVELLE_DEMANDE));
        EvaluationV11 evaluation = Matrice.recevoir(SERVICE_1_0, EvaluationV11.class);
        assertNull(evaluation.motif());
    }

    @Test
    @DisplayName("Nouveau client, nouveau service")
    void nouveauClient_nouveauService() {
        assertEquals(List.of(), Matrice.envoyer(SERVICE_1_1, Matrice.NOUVELLE_DEMANDE));
        EvaluationV11 evaluation = Matrice.recevoir(SERVICE_1_1, EvaluationV11.class);
        assertNotNull(evaluation.motif());
    }
}
