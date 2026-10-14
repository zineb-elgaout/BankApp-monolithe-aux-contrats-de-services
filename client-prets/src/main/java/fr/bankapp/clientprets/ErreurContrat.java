package fr.bankapp.clientprets;

/** Erreur renvoyée par le service : un code du contrat (CLIENT_INCONNU...) et le statut HTTP. */
public class ErreurContrat extends RuntimeException {

    private final int statut;
    private final String code;

    public ErreurContrat(int statut, String code, String detail) {
        super("HTTP " + statut + " " + code + (detail.isBlank() ? "" : " : " + detail));
        this.statut = statut;
        this.code = code;
    }

    public int statut() {
        return statut;
    }

    public String code() {
        return code;
    }
}
