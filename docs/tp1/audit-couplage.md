# Audit de couplage de BankApp (étape 1)

## Mesures brutes

| Mesure | Valeur | Outil |
|---|---|---|
| Dépendances croisées entre applications, vues par jdeps | 13 | `jdeps -verbose:package` |
| ... dont vers une classe interne d'une autre application | 11 | jdeps |
| Accès SQL au schéma d'une autre application (invisibles pour jdeps) | 3 | `git grep "_DB\."` |
| Appels directs à la librairie du fournisseur SMS | 4 | `git grep "new AcmeSms"` |
| Copies du calcul du risque | 4 (Risque, Prêts, CRM, Mobile) | lecture du code |

## Classement selon les quatre dimensions du couplage (chapitre 1, slide 25)

Une croix signale une dimension où le couplage est **fort**.

| # | De | Vers | Élément utilisé | Vu par jdeps | Technologie | Localisation | Données | Temps |
|---|---|---|---|---|---|---|---|---|
| 1 | Prêts | Comptes | `CompteDAO.getSolde()` (classe interne) | oui | X | | | X |
| 2 | Prêts | Risque | `SELECT score FROM RISK_DB.T_SCORE` | **non** | | | X | X |
| 3 | Prêts | CRM | `ClientDAO.getTelephone()` (classe interne) | oui | X | | | X |
| 4 | Prêts | Acme SMS | `new AcmeSms("10.2.4.17")` | oui | X | X | | X |
| 5 | Comptes | Prêts | `EcheanceDAO.prochaines()` (classe interne) | oui | X | | | X |
| 6 | Comptes | Risque | `SurveillanceService.analyser()` (service public) | oui | X | | | X |
| 7 | Comptes | CRM | `ClientDAO.getTelephone()` (classe interne) | oui | X | | | X |
| 8 | Comptes | Acme SMS | `new AcmeSms("10.2.4.17")` | oui | X | X | | X |
| 9 | Risque | Comptes | `MouvementDAO.retraitsImportants()` (classe interne) | oui | X | | | X |
| 10 | Risque | CRM | `ClientDAO.getDateNaissance()` (classe interne) | oui | X | | | X |
| 11 | CRM | Comptes | `CompteDAO.getSolde()` (classe interne) | oui | X | | | X |
| 12 | CRM | Risque | `SELECT SCORE FROM RISK_DB.T_SCORE` | **non** | | | X | X |
| 13 | CRM | Risque | `Seuils.scoreAccepte()` (classe interne) | oui | X | | | X |
| 14 | CRM | Acme SMS | `new AcmeSms("10.2.4.17")` | oui | X | X | | X |
| 15 | Mobile | Comptes | `CompteService.consulterSolde()` (service public) | oui | X | | | X |
| 16 | Mobile | Comptes | `SELECT ... FROM CPT_DB.T_COMPTE` | **non** | | | X | X |
| 17 | Mobile | Risque | `ScoreDAO.scoreDeBase()` (classe interne) | oui | X | | | X |
| 18 | Mobile | CRM | `ClientDAO.getTelephone()` (classe interne) | oui | X | | | X |
| 19 | Mobile | Prêts | `EcheanceDAO.prochaines()` (classe interne) | oui | X | | | X |
| 20 | Mobile | Acme SMS | `new AcmeSms("10.2.4.17")` | oui | X | X | | X |

## Constats

1. **Toutes** les dépendances sont fortes sur la dimension temps : chaque appel est synchrone, dans le même processus.
2. Trois dépendances échappent à jdeps : un outil d'analyse du bytecode ne voit pas le SQL.
3. Le calcul du risque existe en quatre exemplaires, avec quatre seuils différents. Pour la cliente C-1005, Risque répond REFUSE, Mobile donne un pré-accord et Prêts accorde le prêt.
4. Risque est la cible naturelle d'une première extraction : quatre consommateurs, une règle dupliquée, des données lues par d'autres.
