# Avis d'architecte : extraction du service Risque de BankApp

**Recommandation.** Généraliser le service Risque à tous les consommateurs, puis extraire Comptes.

**Ce qu'on a gagné.** Les trois consommateurs (Prêts, CRM, Mobile) ne connaissent plus que le contrat
`evaluerRisque` : 3 dépendances et 2 accès SQL supprimés, 3 cycles cassés sur 8. La règle du risque n'existe
plus qu'une fois : la cliente C-1005 obtenait trois réponses contradictoires, elle en obtient une seule.
Quand l'équipe Risque renomme une colonne, plus aucun test ne casse hors de Risque (5 avant).

**Ce que ça coûte.** Un appel distant est 2 à 3 fois plus lent qu'un appel local sur la même machine, et
davantage sur un vrai réseau. Surtout, le service peut tomber : service arrêté, aucune demande de prêt
n'aboutit. Le couplage temporel est désormais visible, il faudra le traiter (délai d'attente, nouvelle
tentative, mode dégradé, échanges asynchrones). Il y a un processus de plus à déployer et à surveiller,
et une base à synchroniser (scores, incidents, dates de naissance copiées depuis le monolithe).

**Quel service extraire ensuite.** Comptes : les 5 cycles restants passent tous par lui, et Prêts, CRM
et Mobile lisent encore ses classes internes ou sa table T_COMPTE. Ensuite, Notification : quatre
applications appellent encore la librairie Acme et son adresse 10.2.4.17 en dur.
