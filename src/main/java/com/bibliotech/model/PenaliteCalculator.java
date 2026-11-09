package com.bibliotech.model;

import java.time.LocalDate;

/** Calcul des pénalités de retard selon le statut d'un emprunt. */
public final class PenaliteCalculator {

    public static final double TARIF_PAR_JOUR = 0.50;     // en euros
    public static final double PLAFOND_EN_RETARD = 30.00; // en euros

    private PenaliteCalculator() {
        // Classe utilitaire : pas d'instanciation
    }

    /**
     * Calcule la pénalité (en euros) associée à un statut d'emprunt.
     * Switch expression à pattern matching : aucun if/else sur instanceof.
     */
    public static double calculerPenalite(StatutEmprunt statut) {
        return switch (statut) {
            case null ->
                    throw new IllegalArgumentException("Le statut ne peut pas être null.");

            // Emprunt en cours : pas de pénalité
            case StatutEmprunt.EnCours enCours -> 0.00;

            // Rendu à temps : pas de pénalité (garde "when")
            case StatutEmprunt.Rendu rendu when !rendu.enRetardLorsDuRetour() -> 0.00;

            // Rendu en retard : 0,50 € par jour de retard
            case StatutEmprunt.Rendu rendu -> rendu.joursDeRetard() * TARIF_PAR_JOUR;

            // En retard (non rendu) : 0,50 €/jour, plafonné à 30 € — record pattern (déconstruction)
            case StatutEmprunt.EnRetard(LocalDate dateLimite, long joursDeRetard) ->
                    Math.min(joursDeRetard * TARIF_PAR_JOUR, PLAFOND_EN_RETARD);
        };
    }
}