package com.bibliotech.model;

import java.time.Year;

public record Livre(long id,
                    String titre,
                    String auteur,
                    int anneePublication,
                    int exemplairesTotal,
                    int exemplairesDisponibles) {

    public static final int ANNEE_MIN = 1500;

    /** Constructeur compact : collecte TOUTES les erreurs, puis lève une seule exception. */
    public Livre {
        int anneeCourante = Year.now().getValue();

        new Validations()
                .verifierNonVide(titre, "titre", "Le titre est obligatoire.")
                .verifierNonVide(auteur, "auteur", "L'auteur est obligatoire.")
                .verifier(anneePublication >= ANNEE_MIN && anneePublication <= anneeCourante,
                        "anneePublication",
                        "L'année doit être comprise entre " + ANNEE_MIN + " et " + anneeCourante + ".")
                .verifier(exemplairesTotal >= 0, "exemplairesTotal",
                        "Le nombre total d'exemplaires ne peut pas être négatif.")
                .verifier(exemplairesDisponibles >= 0, "exemplairesDisponibles",
                        "Le nombre d'exemplaires disponibles ne peut pas être négatif.")
                .verifier(exemplairesDisponibles <= exemplairesTotal, "exemplairesDisponibles",
                        "Les exemplaires disponibles ne peuvent pas dépasser le nombre total.")
                .leverSiErreurs();

        // Normalisation (après validation : titre et auteur ne sont pas null ici)
        titre = titre.strip();
        auteur = auteur.strip();
    }
}