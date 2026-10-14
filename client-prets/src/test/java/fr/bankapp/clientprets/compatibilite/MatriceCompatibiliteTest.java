package fr.bankapp.clientprets.compatibilite;

import fr.bankapp.contrat.ContratOpenApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

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
        fail("À écrire à l'étape 7");
    }

    @Test
    @DisplayName("Ancien client, nouveau service : rétrocompatibilité (backward)")
    void ancienClient_nouveauService() {
        fail("À écrire à l'étape 7");
    }

    @Test
    @DisplayName("Nouveau client, ancien service : compatibilité anticipée (forward)")
    void nouveauClient_ancienService() {
        fail("À écrire à l'étape 7");
    }

    @Test
    @DisplayName("Nouveau client, nouveau service")
    void nouveauClient_nouveauService() {
        fail("À écrire à l'étape 7");
    }
}
