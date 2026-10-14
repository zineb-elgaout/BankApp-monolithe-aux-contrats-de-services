package fr.bankapp.clientprets.compatibilite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** La réponse telle que la lit un client écrit pour la version 1.1 : il connaît le motif. */
@JsonIgnoreProperties(ignoreUnknown = true)
record EvaluationV11(int score, String decision, String motif) {
}
