package fr.bankapp.contrat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.networknt.schema.ValidationMessage;

/**
 * Erreur normalisée renvoyée quand un message ne respecte pas son schéma.
 * Format du cours : { "code": "MESSAGE_INVALIDE", "champ": ..., "regle": ..., "recu": ... }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErreurMessage(String code, String champ, String regle, Object recu) {

    public static final String MESSAGE_INVALIDE = "MESSAGE_INVALIDE";

    /** Traduit une violation de schéma en erreur lisible par le consommateur. */
    public static ErreurMessage depuis(ValidationMessage violation) {
        String champ = violation.getInstanceLocation().toString().replaceFirst("^\\$\\.?", "");
        String regle;
        Object recu;
        if (violation.getType().equals("required")) {
            champ = (champ.isEmpty() ? "" : champ + ".") + violation.getProperty();
            regle = "obligatoire";
            recu = null;
        } else {
            regle = violation.getType() + " " + texte(violation.getSchemaNode());
            recu = violation.getInstanceNode() == null ? null
                    : violation.getInstanceNode().isTextual() ? violation.getInstanceNode().asText()
                    : violation.getInstanceNode();
        }
        return new ErreurMessage(MESSAGE_INVALIDE, champ.isEmpty() ? "(message)" : champ, regle.trim(), recu);
    }

    private static String texte(com.fasterxml.jackson.databind.JsonNode noeud) {
        if (noeud == null) {
            return "";
        }
        if (noeud.isArray()) {
            java.util.List<String> valeurs = new java.util.ArrayList<>();
            noeud.forEach(v -> valeurs.add(v.asText()));
            return "[" + String.join(", ", valeurs) + "]";
        }
        return noeud.isTextual() ? noeud.asText() : noeud.toString();
    }
}
