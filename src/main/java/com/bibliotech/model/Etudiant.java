package com.bibliotech.model;

import java.util.regex.Pattern;

public record Etudiant(long id,
                       String numeroEtudiant,
                       String nom,
                       String prenom,
                       String email) {

    /** 2 lettres majuscules suivies de 6 chiffres (ex. ET123456). */
    private static final Pattern FORMAT_NUMERO = Pattern.compile("[A-Z]{2}\\d{6}");

    /** Constructeur compact : validation des contraintes métier. */
    public Etudiant {
        Validations.nonVide(numeroEtudiant, "numéro étudiant");
        Validations.nonVide(nom, "nom");
        Validations.nonVide(prenom, "prénom");
        Validations.nonVide(email, "e-mail");

        Validations.exiger(FORMAT_NUMERO.matcher(numeroEtudiant).matches(),
                "Le numéro étudiant doit comporter 2 lettres majuscules suivies de 6 chiffres (ex. ET123456).");

        Validations.exiger(email.contains("@"),
                "L'adresse e-mail doit contenir un « @ ».");

        // Normalisation
        nom = nom.strip();
        prenom = prenom.strip();
        email = email.strip();
    }
}