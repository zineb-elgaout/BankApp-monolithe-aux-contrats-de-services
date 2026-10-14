package fr.bankapp.risqueservice.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La validation en entrée, sur les exemples de contrats/exemples. */
@Tag("validation")
class ValidateurDemandeTest {

    static final Path CONTRATS = Path.of("../contrats");
    final ValidateurDemande validateur =
            new ValidateurDemande(CONTRATS.resolve("risque.yaml"), CONTRATS.resolve("xml/demande-pret.xsd"));

    static String exemple(String nom) throws IOException {
        return Files.readString(CONTRATS.resolve("exemples").resolve(nom));
    }

    @Test
    @DisplayName("JSON : une demande conforme est acceptée")
    void jsonConforme() throws IOException {
        assertEquals(Optional.empty(), validateur.validerJson(exemple("demande-valide.json")));
    }

    @Test
    @DisplayName("JSON : une durée de 2 mois est refusée, avec le champ, la règle et la valeur reçue")
    void jsonDureeInvalide() throws IOException {
        ErreurValidation erreur = validateur.validerJson(exemple("demande-duree-invalide.json")).orElseThrow();
        assertEquals("MESSAGE_INVALIDE", erreur.code());
        assertEquals("dureeMois", erreur.champ());
        assertEquals("minimum 6", erreur.regle());
        assertEquals("2", String.valueOf(erreur.recu()));
    }

    @Test
    @DisplayName("JSON : un montant envoyé en nombre est refusé")
    void jsonMontantNombre() throws IOException {
        ErreurValidation erreur = validateur.validerJson(exemple("demande-montant-nombre.json")).orElseThrow();
        assertEquals("montant", erreur.champ());
        assertEquals("type string", erreur.regle());
    }

    @Test
    @DisplayName("XML : une demande conforme est acceptée")
    void xmlConforme() throws IOException {
        assertEquals(Optional.empty(), validateur.validerXml(exemple("demande-valide.xml")));
    }

    @Test
    @DisplayName("XML : une durée de 2 mois est refusée, avec le champ en cause")
    void xmlDureeInvalide() throws IOException {
        ErreurValidation erreur = validateur.validerXml(exemple("demande-duree-invalide.xml")).orElseThrow();
        assertEquals("MESSAGE_INVALIDE", erreur.code());
        assertEquals("dureeMois", erreur.champ());
        assertTrue(erreur.regle().contains("6"), "la règle doit citer la borne 6 : " + erreur.regle());
    }
}
