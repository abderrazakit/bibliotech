package com.bibliotech.dao;

/** Erreur technique d'accès aux données (encapsule une SQLException). */
public class DAOException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }
}