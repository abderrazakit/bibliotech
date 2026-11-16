package com.bibliotech.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Entité JPA mappée sur la table "emprunt" : c'est le côté propriétaire des deux relations. */
@Entity
@Table(name = "emprunt")
public class Emprunt {

    /**
     * Statut stocké en base (VARCHAR).
     * EN_RETARD n'est jamais enregistré : la contrainte CHECK de la table n'accepte que
     * EN_COURS et RENDU. Le retard dépend de la date du jour, il est donc calculé (voir statutMetier).
     */
    public enum Statut { EN_COURS, RENDU, EN_RETARD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livre_id", nullable = false)         // la clé étrangère est ici
    private Livre livre;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @NotNull(message = "La date d'emprunt est obligatoire.")
    @Column(name = "date_emprunt", nullable = false)
    private LocalDate dateEmprunt;

    @NotNull(message = "La date de retour prévue est obligatoire.")
    @Column(name = "date_retour_prevue", nullable = false)
    private LocalDate dateRetourPrevue;

    @Column(name = "date_retour_effective")                  // null tant que le livre n'est pas rendu
    private LocalDate dateRetourEffective;

    @NotNull
    @Enumerated(EnumType.STRING)                             // stocke "EN_COURS", pas 0
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.EN_COURS;

    protected Emprunt() {}

    public Emprunt(Livre livre, Etudiant etudiant, LocalDate dateEmprunt, LocalDate dateRetourPrevue) {
        this.livre = livre;
        this.etudiant = etudiant;
        this.dateEmprunt = dateEmprunt;
        this.dateRetourPrevue = dateRetourPrevue;
        this.statut = Statut.EN_COURS;
    }

    // ===== Règles de cohérence (Bean Validation) =====

    @AssertTrue(message = "La date de retour prévue ne peut pas précéder la date d'emprunt.")
    public boolean isDatesCoherentes() {
        return dateEmprunt == null || dateRetourPrevue == null || !dateRetourPrevue.isBefore(dateEmprunt);
    }

    @AssertTrue(message = "La date de retour effective ne peut pas précéder la date d'emprunt.")
    public boolean isRetourCoherent() {
        return dateRetourEffective == null || dateEmprunt == null || !dateRetourEffective.isBefore(dateEmprunt);
    }

    // ===== Pont vers le modèle métier du TP 1 =====

    /**
     * Recalcule le statut "riche" (sealed interface du TP 1) à partir des dates.
     * Pas stocké en base : il dépend de la date du jour.
     */
    public StatutEmprunt statutMetier(LocalDate aujourdhui) {
        if (dateRetourEffective != null) {
            long jours = Math.max(0, ChronoUnit.DAYS.between(dateRetourPrevue, dateRetourEffective));
            return new StatutEmprunt.Rendu(dateRetourEffective, jours > 0, jours);
        }
        if (aujourdhui.isAfter(dateRetourPrevue)) {
            return new StatutEmprunt.EnRetard(dateRetourPrevue,
                    ChronoUnit.DAYS.between(dateRetourPrevue, aujourdhui));
        }
        return new StatutEmprunt.EnCours(dateRetourPrevue);
    }

    public StatutEmprunt statutMetier() {
        return statutMetier(LocalDate.now());
    }

    // ===== Getters / setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Livre getLivre() { return livre; }
    public void setLivre(Livre livre) { this.livre = livre; }

    public Etudiant getEtudiant() { return etudiant; }
    public void setEtudiant(Etudiant etudiant) { this.etudiant = etudiant; }

    public LocalDate getDateEmprunt() { return dateEmprunt; }
    public void setDateEmprunt(LocalDate dateEmprunt) { this.dateEmprunt = dateEmprunt; }

    public LocalDate getDateRetourPrevue() { return dateRetourPrevue; }
    public void setDateRetourPrevue(LocalDate dateRetourPrevue) { this.dateRetourPrevue = dateRetourPrevue; }

    public LocalDate getDateRetourEffective() { return dateRetourEffective; }
    public void setDateRetourEffective(LocalDate dateRetourEffective) {
        this.dateRetourEffective = dateRetourEffective;
    }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
}