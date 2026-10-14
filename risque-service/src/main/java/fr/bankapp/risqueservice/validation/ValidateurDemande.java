package fr.bankapp.risqueservice.validation;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Validation en entrée du service Risque : une demande de prêt, en JSON ou en XML,
 * est confrontée au contrat avant tout traitement. Un service ne fait jamais confiance
 * à ce qu'il reçoit (chapitre 3, slide 24).
 */
public class ValidateurDemande {

    /**
     * @param contrat le contrat OpenAPI qui contient le schéma DemandePretDto (contrats/risque.yaml)
     * @param xsd     le schéma XSD de la demande en XML (contrats/xml/demande-pret.xsd)
     */
    public ValidateurDemande(Path contrat, Path xsd) {
        // À COMPLÉTER À L'ÉTAPE 6 : charger les deux schémas, une seule fois.
    }

    /** Retourne la première erreur trouvée, ou rien si la demande JSON respecte le contrat. */
    public Optional<ErreurValidation> validerJson(String demande) {
        // À COMPLÉTER À L'ÉTAPE 6
        throw new UnsupportedOperationException("Validation JSON non écrite (étape 6)");
    }

    /** Retourne la première erreur trouvée, ou rien si la demande XML respecte le schéma XSD. */
    public Optional<ErreurValidation> validerXml(String demande) {
        // À COMPLÉTER À L'ÉTAPE 6
        throw new UnsupportedOperationException("Validation XML non écrite (étape 6)");
    }
}
