package com.bibliotech.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception de validation contenant TOUTES les erreurs, champ par champ.
 * Hérite de IllegalArgumentException : elle reste une IllegalArgumentException.
 */
public class ValidationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /** Clé = nom du champ (ex. "titre"), valeur = message d'erreur. */
    private final Map<String, String> erreurs;

    public ValidationException(Map<String, String> erreurs) {
        super(String.join(" ", erreurs.values()));   // message global = tous les messages
        this.erreurs = Collections.unmodifiableMap(new LinkedHashMap<>(erreurs));
    }

    public Map<String, String> getErreurs() {
        return erreurs;
    }
}