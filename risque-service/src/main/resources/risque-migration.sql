-- Migration des données depuis RISK_DB et CRM_DB du monolithe.
-- En production, ce script serait une extraction suivie d'une synchronisation.
INSERT INTO T_SCORE VALUES ('C-1001', 72), ('C-1002', 68), ('C-1003', 45), ('C-1004', 61), ('C-1005', 64), ('C-1006', 78);
INSERT INTO T_INCIDENT VALUES
    ('C-1005', DATEADD('MONTH', -2, CURRENT_DATE)),
    ('C-1005', DATEADD('MONTH', -5, CURRENT_DATE));
INSERT INTO T_PROFIL VALUES
    ('C-1001', DATEADD('YEAR', -41, CURRENT_DATE)),
    ('C-1002', DATEADD('YEAR', -36, CURRENT_DATE)),
    ('C-1003', DATEADD('YEAR', -22, CURRENT_DATE)),
    ('C-1004', DATEADD('YEAR', -48, CURRENT_DATE)),
    ('C-1005', DATEADD('YEAR', -31, CURRENT_DATE)),
    ('C-1006', DATEADD('YEAR', -44, CURRENT_DATE));
