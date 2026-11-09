package com.bibliotech.model;

import java.time.LocalDate;

public sealed interface StatutEmprunt
        permits StatutEmprunt.EnCours,
                StatutEmprunt.Rendu,
                StatutEmprunt.EnRetard {

    /** Emprunt en cours, à rendre au plus tard à dateLimite. */
    record EnCours(LocalDate dateLimite) implements StatutEmprunt {
        public EnCours {
            Validations.exiger(dateLimite != null, "La date limite est obligatoire.");
        }
    }

    /**
     * Livre rendu à dateRetour.
     * Écart par rapport au sujet : ajout de joursDeRetard, afin de pouvoir
     * appliquer la pénalité de 0,50 €/jour (« si vous avez stocké le retard »).
     */
    record Rendu(LocalDate dateRetour, boolean enRetardLorsDuRetour, long joursDeRetard)
            implements StatutEmprunt {
        public Rendu {
            Validations.exiger(dateRetour != null, "La date de retour est obligatoire.");
            Validations.exiger(joursDeRetard >= 0, "Le nombre de jours de retard ne peut pas être négatif.");
            Validations.exiger(enRetardLorsDuRetour == (joursDeRetard > 0),
                    "Incohérence : enRetardLorsDuRetour doit être vrai si et seulement si joursDeRetard > 0.");
        }
    }

    /** Date limite dépassée de joursDeRetard jours, livre non rendu. */
    record EnRetard(LocalDate dateLimite, long joursDeRetard) implements StatutEmprunt {
        public EnRetard {
            Validations.exiger(dateLimite != null, "La date limite est obligatoire.");
            Validations.exiger(joursDeRetard > 0, "Un emprunt en retard a au moins 1 jour de retard.");
        }
    }
}