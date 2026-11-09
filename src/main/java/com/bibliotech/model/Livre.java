package com.bibliotech.model;

import java.time.Year;

public record Livre(long id,
                    String titre,
                    String auteur,
                    int anneePublication,
                    int exemplairesTotal,
                    int exemplairesDisponibles) {

    public static final int ANNEE_MIN = 1500;

    /** Constructeur compact : validation des contraintes métier. */
    public Livre {
        Validations.nonVide(titre, "titre");
        Validations.nonVide(auteur, "auteur");

        int anneeCourante = Year.now().getValue();
        Validations.exiger(anneePublication >= ANNEE_MIN && anneePublication <= anneeCourante,
                "L'année de publication doit être comprise entre "
                + ANNEE_MIN + " et " + anneeCourante + ".");

        Validations.exiger(exemplairesTotal >= 0,
                "Le nombre total d'exemplaires ne peut pas être négatif.");
        Validations.exiger(exemplairesDisponibles >= 0,
                "Le nombre d'exemplaires disponibles ne peut pas être négatif.");
        Validations.exiger(exemplairesDisponibles <= exemplairesTotal,
                "Les exemplaires disponibles ne peuvent pas dépasser le nombre total.");

        // Normalisation : on retire les espaces superflus au début et à la fin
        titre = titre.strip();
        auteur = auteur.strip();
    }
}