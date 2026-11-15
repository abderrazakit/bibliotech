package com.bibliotech.model;

/** Exception métier : plus aucun exemplaire disponible pour ce livre. */
public class LivreIndisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long livreId;

    public LivreIndisponibleException(long livreId) {
        super("Le livre " + livreId + " n'a plus d'exemplaire disponible.");
        this.livreId = livreId;
    }

    public long getLivreId() {
        return livreId;
    }
}