package com.bibliotech.web;

/** Une ligne du tableau des emprunts, déjà formatée pour l'affichage (JSP sans logique). */
public record LigneEmprunt(long id,
                           String titreLivre,
                           String etudiant,
                           String dateEmprunt,
                           String dateRetourPrevue,
                           String statut,
                           boolean enRetard,
                           String penalite) {
}