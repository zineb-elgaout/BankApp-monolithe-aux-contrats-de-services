package fr.bankapp;

import fr.bankapp.commun.BaseDeDonnees;
import fr.bankapp.comptes.VirementService;
import fr.bankapp.contrats.ServiceRisque;
import fr.bankapp.crm.CampagneService;
import fr.bankapp.integration.AiguillageRisque;
import fr.bankapp.mobile.AppMobileService;
import fr.bankapp.mobile.SimulationMobileService;
import fr.bankapp.prets.Decision;
import fr.bankapp.prets.Demande;
import fr.bankapp.prets.PretService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Point d'entrée de BankApp.
 * Sans argument : une journée type à la banque.
 * Avec l'argument « guichet » : le guichet des prêts traite une demande toutes les deux secondes.
 */
public class Main {

    /** Le seul endroit qui choisit la réalisation du contrat ServiceRisque. */
    static final ServiceRisque RISQUE = new AiguillageRisque();

    public static void main(String[] args) throws InterruptedException {
        BaseDeDonnees.initialiser();
        if (args.length > 0 && args[0].equals("guichet")) {
            guichet();
        } else {
            journee();
        }
    }

    static void journee() {
        System.out.println("=== Demandes de prêt ===");
        PretService prets = new PretService(RISQUE);
        afficher("C-1001", prets.traiter(new Demande("C-1001", new BigDecimal("20000"), 24)));
        afficher("C-1002", prets.traiter(new Demande("C-1002", new BigDecimal("10000"), 12)));

        System.out.println("=== Opérations courantes ===");
        new VirementService().virer("CPT-004", "CPT-001", new BigDecimal("50"));
        List<String> cibles = new CampagneService(RISQUE).proposerCredit(new BigDecimal("20000"), 36);
        System.out.println("Campagne crédit envoyée à : " + cibles);
        new AppMobileService().envoyerCodeConnexion("C-1006");

        System.out.println("=== Un client, plusieurs réponses : Nadia Fassi (C-1005), 20000 sur 36 mois ===");
        BigDecimal montant = new BigDecimal("20000");
        System.out.println("Service Risque (contrat)      : " + RISQUE.evaluerRisque("C-1005", montant, 36));
        System.out.println("Application Mobile            : pré-accord = "
                + new SimulationMobileService(RISQUE).preAccord("C-1005", montant, 36));
        System.out.println("Application CRM               : ciblée par la campagne = " + cibles.contains("C-1005"));
        afficher("C-1005", prets.traiter(new Demande("C-1005", montant, 36)));
    }

    static void guichet() throws InterruptedException {
        PretService prets = new PretService(RISQUE);
        List<Demande> file = List.of(
                new Demande("C-1001", new BigDecimal("20000"), 24),
                new Demande("C-1006", new BigDecimal("50000"), 60),
                new Demande("C-1003", new BigDecimal("5000"), 12),
                new Demande("C-1005", new BigDecimal("20000"), 36));
        System.out.println("Guichet des prêts ouvert. Ctrl+C pour fermer.");
        for (int i = 0; ; i++) {
            Demande demande = file.get(i % file.size());
            try {
                afficher(demande.idClient(), prets.traiter(demande));
            } catch (RuntimeException e) {
                System.out.println("[guichet] " + demande.idClient() + " : ÉCHEC, " + e.getMessage());
            }
            Thread.sleep(2000);
        }
    }

    static void afficher(String idClient, Decision decision) {
        System.out.println("[Prêts] " + idClient + " : " + decision.statut() + " (" + decision.motif() + ")");
    }
}
