package com.bibliotech.model;

/** Méthodes de validation partagées par les records du modèle. */
final class Validations {

    private Validations() {
        // Classe utilitaire : pas d'instanciation
    }

    /** Lève une IllegalArgumentException si la condition est fausse. */
    static void exiger(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Vérifie qu'une chaîne n'est ni null, ni vide, ni composée uniquement d'espaces. */
    static void nonVide(String valeur, String nomDuChamp) {
        exiger(valeur != null && !valeur.isBlank(),
               "Le champ « " + nomDuChamp + " » est obligatoire.");
    }
}