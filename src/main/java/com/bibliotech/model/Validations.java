package com.bibliotech.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Outils de validation partagés par les records du modèle. */
final class Validations {

    // ========== Mode "collecte" : accumule TOUTES les erreurs, champ par champ ==========

    private final Map<String, String> erreurs = new LinkedHashMap<>();

    /** Enregistre une erreur pour ce champ si la condition est fausse (1re erreur par champ). */
    Validations verifier(boolean condition, String champ, String message) {
        if (!condition) {
            erreurs.putIfAbsent(champ, message);
        }
        return this;   // permet d'enchaîner les appels
    }

    /** Vérifie qu'une chaîne n'est ni null, ni vide, ni composée uniquement d'espaces. */
    Validations verifierNonVide(String valeur, String champ, String message) {
        return verifier(valeur != null && !valeur.isBlank(), champ, message);
    }

    /** Lève une ValidationException contenant toutes les erreurs, s'il y en a. */
    void leverSiErreurs() {
        if (!erreurs.isEmpty()) {
            throw new ValidationException(erreurs);
        }
    }

    // ========== Mode "immédiat" : lève une exception dès la 1re erreur ==========

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