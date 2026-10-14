package fr.bankapp.clientprets;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

/**
 * Objet d'échange : la demande envoyée au service Risque. Il a exactement la forme du message.
 * Le montant est un BigDecimal en Java, mais il voyage en chaîne décimale : "20000.00".
 */
public record DemandePretDto(
        String idClient,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal montant,
        int dureeMois) {
}
