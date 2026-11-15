-- =====================================================================
--  BiblioTech — schéma PostgreSQL + données de démonstration
--  Exécution : base "bibliotech", en tant que rôle "bibliotech_app"
-- =====================================================================

-- Suppression dans l'ordre inverse des dépendances (script rejouable)
DROP TABLE IF EXISTS emprunt;
DROP TABLE IF EXISTS etudiant;
DROP TABLE IF EXISTS livre;

-- ---------------------------------------------------------------------
--  LIVRE
-- ---------------------------------------------------------------------
CREATE TABLE livre (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titre                   VARCHAR(255) NOT NULL,
    auteur                  VARCHAR(255) NOT NULL,
    annee_publication       INTEGER      NOT NULL,
    exemplaires_total       INTEGER      NOT NULL,
    exemplaires_disponibles INTEGER      NOT NULL,
    CONSTRAINT ck_livre_total_positif CHECK (exemplaires_total >= 0),
    CONSTRAINT ck_livre_dispo_max     CHECK (exemplaires_disponibles <= exemplaires_total)
    -- NB : la contrainte "exemplaires_disponibles >= 0" est volontairement
    --      absente : elle fait l'objet du Bonus 1 (race condition).
);

-- ---------------------------------------------------------------------
--  ETUDIANT
-- ---------------------------------------------------------------------
CREATE TABLE etudiant (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_etudiant  VARCHAR(8)   NOT NULL UNIQUE,
    nom              VARCHAR(100) NOT NULL,
    prenom           VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    CONSTRAINT ck_etudiant_numero CHECK (numero_etudiant ~ '^[A-Z]{2}[0-9]{6}$')
);

-- ---------------------------------------------------------------------
--  EMPRUNT
-- ---------------------------------------------------------------------
CREATE TABLE emprunt (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    livre_id              BIGINT      NOT NULL REFERENCES livre(id),
    etudiant_id           BIGINT      NOT NULL REFERENCES etudiant(id),
    date_emprunt          DATE        NOT NULL,
    date_retour_prevue    DATE        NOT NULL,
    date_retour_effective DATE,                       -- NULL tant que non rendu
    statut                VARCHAR(20) NOT NULL,
    CONSTRAINT ck_emprunt_statut CHECK (statut IN ('EN_COURS', 'RENDU')),
    CONSTRAINT ck_emprunt_dates  CHECK (date_retour_prevue >= date_emprunt)
);

-- Index sur les clés étrangères (accélère les jointures et les recherches)
CREATE INDEX idx_emprunt_livre    ON emprunt(livre_id);
CREATE INDEX idx_emprunt_etudiant ON emprunt(etudiant_id);

-- =====================================================================
--  DONNÉES DE DÉMONSTRATION
-- =====================================================================

-- 5 livres de démo (exigés par le sujet)
INSERT INTO livre (titre, auteur, annee_publication, exemplaires_total, exemplaires_disponibles) VALUES
    ('Effective Java',  'Joshua Bloch',              2018, 3, 3),
    ('Clean Code',      'Robert C. Martin',          2008, 2, 1),
    ('Design Patterns', 'Erich Gamma',               1994, 2, 2),
    ('Le Petit Prince', 'Antoine de Saint-Exupéry',  1943, 4, 4),
    ('Les Misérables',  'Victor Hugo',               1862, 1, 0);

-- 3 étudiants
INSERT INTO etudiant (numero_etudiant, nom, prenom, email) VALUES
    ('ET123456', 'Martin',  'Alice', 'alice.martin@univ.fr'),
    ('ET234567', 'Durand',  'Bob',   'bob.durand@univ.fr'),
    ('ET345678', 'Bernard', 'Chloé', 'chloe.bernard@univ.fr');

-- 3 emprunts, cohérents avec les exemplaires disponibles ci-dessus
INSERT INTO emprunt (livre_id, etudiant_id, date_emprunt, date_retour_prevue, date_retour_effective, statut) VALUES
    -- Clean Code (1 dispo / 2) : emprunt EN COURS par Alice, à rendre dans 7 jours
    ((SELECT id FROM livre WHERE titre = 'Clean Code'),
     (SELECT id FROM etudiant WHERE numero_etudiant = 'ET123456'),
     CURRENT_DATE - 7, CURRENT_DATE + 7, NULL, 'EN_COURS'),

    -- Les Misérables (0 dispo / 1) : emprunt EN RETARD de 5 jours par Bob
    ((SELECT id FROM livre WHERE titre = 'Les Misérables'),
     (SELECT id FROM etudiant WHERE numero_etudiant = 'ET234567'),
     CURRENT_DATE - 19, CURRENT_DATE - 5, NULL, 'EN_COURS'),

    -- Effective Java : emprunt RENDU avec 3 jours de retard par Chloé
    ((SELECT id FROM livre WHERE titre = 'Effective Java'),
     (SELECT id FROM etudiant WHERE numero_etudiant = 'ET345678'),
     CURRENT_DATE - 30, CURRENT_DATE - 16, CURRENT_DATE - 13, 'RENDU');