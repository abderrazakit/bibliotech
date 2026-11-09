package com.bibliotech.model;

import java.time.LocalDate;

public sealed interface StatutEmprunt
        permits StatutEmprunt.EnCours,
                StatutEmprunt.Rendu,
                StatutEmprunt.EnRetard {

    /** Emprunt en cours, à rendre au plus tard à dateLimite. */
    record EnCours(LocalDate dateLimite) implements StatutEmprunt {}

    /** Livre rendu à dateRetour, en retard ou non. */
    record Rendu(LocalDate dateRetour, boolean enRetardLorsDuRetour) implements StatutEmprunt {}

    /** Date limite dépassée de joursDeRetard jours, livre non rendu. */
    record EnRetard(LocalDate dateLimite, long joursDeRetard) implements StatutEmprunt {}
}