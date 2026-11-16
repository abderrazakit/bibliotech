package com.bibliotech.model;

/** Résultat de la requête GROUP BY : un livre et son nombre d'emprunts en cours. */
public class LivreAvecEmprunts {

    private final Livre livre;
    private final long empruntsEnCours;

    /** Appelé par Hibernate dans la requête JPQL "SELECT new ...". COUNT renvoie un Long. */
    public LivreAvecEmprunts(Livre livre, Long empruntsEnCours) {
        this.livre = livre;
        this.empruntsEnCours = empruntsEnCours == null ? 0 : empruntsEnCours;
    }

    public Livre getLivre() {
        return livre;
    }

    public long getEmpruntsEnCours() {
        return empruntsEnCours;
    }
}