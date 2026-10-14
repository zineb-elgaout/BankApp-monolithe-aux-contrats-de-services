package fr.bankapp.clientprets;

import com.sun.net.httpserver.HttpServer;
import fr.bankapp.contrat.SimulateurContrat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le client Prêts contre le service simulé à partir de votre contrat risque.yaml. */
@Tag("contrat")
class ContratRisqueTest {

    static HttpServer simulateur;
    static ClientPrets client;

    @BeforeAll
    static void demarrerLeSimulateur() throws Exception {
        simulateur = SimulateurContrat.demarrer(Path.of("../contrats/risque.yaml"), 0);
        client = new ClientPrets("http://localhost:" + simulateur.getAddress().getPort());
    }

    @AfterAll
    static void arreterLeSimulateur() {
        simulateur.stop(0);
    }

    @Test
    @DisplayName("evaluerRisque répond un score de 0 à 100 et une décision du contrat")
    void evaluationConforme() {
        Evaluation evaluation = client.evaluerRisque(new DemandePretDto("C-1001", new BigDecimal("20000.00"), 24));
        assertTrue(evaluation.score() >= 0 && evaluation.score() <= 100, "score hors de [0, 100]");
        assertTrue(List.of("ACCEPTE", "REFUSE", "A_ETUDIER").contains(evaluation.decision()),
                "décision inconnue : " + evaluation.decision());
    }

    @Test
    @DisplayName("Un client inconnu produit l'erreur CLIENT_INCONNU prévue par le contrat")
    void clientInconnu() {
        ErreurContrat erreur = assertThrows(ErreurContrat.class,
                () -> client.appeler(new DemandePretDto("C-9999", new BigDecimal("20000.00"), 24), "code=404"));
        assertEquals("CLIENT_INCONNU", erreur.code());
    }

    @Test
    @DisplayName("Une demande hors contrat est refusée avec MESSAGE_INVALIDE")
    void demandeHorsContrat() {
        ErreurContrat erreur = assertThrows(ErreurContrat.class,
                () -> client.evaluerRisque(new DemandePretDto("C-1001", new BigDecimal("20000.00"), 2)));
        assertEquals("MESSAGE_INVALIDE", erreur.code());
    }
}
