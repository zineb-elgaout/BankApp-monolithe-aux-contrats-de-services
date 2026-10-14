# Mesures avant et après l'extraction du service Risque

| Mesure | Avant (étape 2) | Après (étape 4) |
|---|---|---|
| Règle 1 : dépendances de Prêts, CRM, Mobile vers l'application Risque | 3 | 0 |
| Règle 2 : accès aux tables de Risque hors de Risque | 2 | 0 |
| Cycles entre applications | 8 | 5 |
| Tests cassés hors de Risque quand l'équipe Risque renomme une colonne | 5 | 0 |
| Temps moyen d'une évaluation du risque (local / distant, même machine) | 0,8 ms | 1,6 à 2,2 ms en distant |
| Demandes de prêt traitées quand le service Risque est arrêté | toutes | aucune (SERVICE_INDISPONIBLE) |
| Réponses différentes pour la cliente C-1005 selon l'application | 3 (REFUSE, pré-accord, ACCORDE) | 1 (REFUSE partout) |
