package fr.bankapp.clientprets.compatibilite;

import fr.bankapp.contrat.ContratOpenApi;
import fr.bankapp.contrat.ErreurMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La version 2 du contrat Risque rend le revenu mensuel obligatoire : c'est un changement cassant.
 * Ces tests documentent la rupture, pour qu'elle reste visible tant que la version 1.1 est servie.
 */
@Tag("compatibilite")
class MatriceVersion2Test {

    static final ContratOpenApi SERVICE_2 = Matrice.contrat("risque-v2.yaml");

    @Test
    @DisplayName("Ancien client, service version 2 : rupture attendue")
    void ancienClient_serviceV2() {
        List<ErreurMessage> erreurs = Matrice.envoyer(SERVICE_2, Matrice.ANCIENNE_DEMANDE);
        assertEquals(1, erreurs.size());
        assertEquals("revenuMensuel", erreurs.get(0).champ());
        assertEquals("obligatoire", erreurs.get(0).regle());
    }

    @Test
    @DisplayName("Nouveau client, service version 2")
    void nouveauClient_serviceV2() {
        assertEquals(List.of(), Matrice.envoyer(SERVICE_2, Matrice.NOUVELLE_DEMANDE));
    }
}
