package fr.bankapp.clientprets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Réponse du service Risque, telle que Prêts la lit.
 * Lecteur tolérant : un champ inconnu ajouté par le service est ignoré, sans erreur.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Evaluation(int score, String decision) {
}
