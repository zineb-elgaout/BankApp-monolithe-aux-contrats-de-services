# BankApp, TP2 : du monolithe aux contrats

Système d'information d'une banque de détail, fil rouge du cours SOA. Ce dépôt reprend l'état final du TP1.

- `monolithe/` : les cinq applications historiques (Comptes, Prêts, Risque, CRM, Mobile).
- `risque-service/` : le service Risque extrait au TP1, et la validation en entrée (étape 6).
- `outils-contrat/` : outils fournis. Inventaire de l'existant, simulateur de service à partir d'un contrat, relecteur de contrats.
- `client-prets/` : le client de l'équipe Prêts, écrit pour le contrat du service Risque, et ses tests.
- `contrats/` : les contrats OpenAPI des services Risque et Notification, les exemples de messages, le schéma XSD.
- `docs/` : vos livrables (candidats.csv), et ceux du TP1 dans docs/tp1.

Lancer les tests : `mvn -q test` (ou `.\mvnw.cmd -q test` sous Windows sans Maven).
