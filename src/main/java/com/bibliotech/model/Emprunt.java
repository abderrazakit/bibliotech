package com.bibliotech.model;

import java.time.LocalDate;

public record Emprunt(long id,
                      long livreId,
                      long etudiantId,
                      LocalDate dateEmprunt,
                      LocalDate dateRetourPrevue,
                      LocalDate dateRetourEffective,   // null tant que le livre n'est pas rendu
                      StatutEmprunt statut) {

    /** Constructeur compact : contrôles de cohérence. */
    public Emprunt {
        Validations.exiger(dateEmprunt != null,
                "La date d'emprunt est obligatoire.");
        Validations.exiger(dateRetourPrevue != null,
                "La date de retour prévue est obligatoire.");
        Validations.exiger(!dateRetourPrevue.isBefore(dateEmprunt),
                "La date de retour prévue ne peut pas précéder la date d'emprunt.");
        Validations.exiger(dateRetourEffective == null || !dateRetourEffective.isBefore(dateEmprunt),
                "La date de retour effective ne peut pas précéder la date d'emprunt.");
        Validations.exiger(statut != null,
                "Le statut de l'emprunt est obligatoire.");
    }
}