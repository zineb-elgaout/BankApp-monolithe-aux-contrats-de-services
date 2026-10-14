package fr.bankapp.risqueservice.validation;

/**
 * Erreur normalisée renvoyée quand une demande ne respecte pas le contrat (chapitre 3, slide 26).
 * { "code": "MESSAGE_INVALIDE", "champ": "dureeMois", "regle": "minimum 6", "recu": 2 }
 */
public record ErreurValidation(String code, String champ, String regle, Object recu) {

    public static final String MESSAGE_INVALIDE = "MESSAGE_INVALIDE";

    public static ErreurValidation messageInvalide(String champ, String regle, Object recu) {
        return new ErreurValidation(MESSAGE_INVALIDE, champ, regle, recu);
    }
}
