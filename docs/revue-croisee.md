# Revue croisée du contrat Risque

Binôme relecteur : (noms)            Binôme auteur : (noms)
Contrat relu : contrats/risque.yaml, version 1.1.0

Le binôme relecteur branche son client Prêts sur le service simulé de l'auteur, puis lit le contrat
comme s'il devait coder le vrai service. Chaque ambiguïté est notée, avec la question qu'elle pose.

| # | Où, dans le contrat | Ambiguïté constatée | Question posée à l'auteur | Décision |
|---|---|---|---|---|
| 1 | DemandePretDto.montant | Le montant peut-il valoir "0.00" ? Le motif l'accepte, mais l'erreur 422 MONTANT_INVALIDE dit « strictement positif ». | Qui refuse 0.00 : le schéma ou le service ? | Le service, en 422 : c'est une règle de gestion. |
| 2 | Evaluation.motif | Le motif est-il destiné au client final, ou à l'agent ? | Peut-on l'afficher tel quel dans l'application mobile ? | À préciser dans la description. |
| 3 | Réponse 200 | La décision A_ETUDIER n'indique ni délai, ni suite à donner. | Que doit faire Prêts d'une demande A_ETUDIER ? | Hors contrat : relève du processus TraiterDemandePret. |
| 4 | info.x-fiche.qualite | « moins de 2 secondes » : en moyenne, ou pour 99 % des appels ? | Quelle mesure fait foi ? | 95 % des appels en moins de 2 secondes. |
