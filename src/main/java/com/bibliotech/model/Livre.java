package com.bibliotech.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/** Entité JPA mappée sur la table "livre". */
@Entity
@Table(name = "livre")
public class Livre {

    public static final int ANNEE_MIN = 1500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // id généré par PostgreSQL
    private Long id;

    @NotBlank(message = "Le titre est obligatoire.")
    @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères.")
    @Column(nullable = false, length = 255)
    private String titre;

    @NotBlank(message = "L'auteur est obligatoire.")
    @Size(max = 255, message = "L'auteur ne doit pas dépasser 255 caractères.")
    @Column(nullable = false, length = 255)
    private String auteur;

    @Min(value = ANNEE_MIN, message = "L'année doit être comprise entre 1500 et l'année en cours.")
    @Column(name = "annee_publication", nullable = false)
    private int anneePublication;

    @Min(value = 0, message = "Le nombre total d'exemplaires ne peut pas être négatif.")
    @Column(name = "exemplaires_total", nullable = false)
    private int exemplairesTotal;

    @Min(value = 0, message = "Le nombre d'exemplaires disponibles ne peut pas être négatif.")
    @Column(name = "exemplaires_disponibles", nullable = false)
    private int exemplairesDisponibles;

    /**
     * Côté inverse de la relation : la clé étrangère est dans "emprunt".
     * Pas de cascade : supprimer un livre ne doit pas effacer son historique d'emprunts.
     */
    @OneToMany(mappedBy = "livre", fetch = FetchType.LAZY)
    private List<Emprunt> emprunts = new ArrayList<>();

    /** Constructeur sans argument exigé par JPA (Hibernate l'utilise pour recréer l'objet). */
    protected Livre() {}

    /** Nouveau livre : tous les exemplaires sont disponibles. */
    public Livre(String titre, String auteur, int anneePublication, int exemplairesTotal) {
        this(titre, auteur, anneePublication, exemplairesTotal, exemplairesTotal);
    }

    public Livre(String titre, String auteur, int anneePublication,
                 int exemplairesTotal, int exemplairesDisponibles) {
        setTitre(titre);
        setAuteur(auteur);
        this.anneePublication = anneePublication;
        this.exemplairesTotal = exemplairesTotal;
        this.exemplairesDisponibles = exemplairesDisponibles;
    }

    // ===== Règles qui portent sur plusieurs champs (Bean Validation) =====

    @AssertTrue(message = "L'année doit être comprise entre 1500 et l'année en cours.")
    public boolean isAnneeNonFuture() {
        return anneePublication <= Year.now().getValue();
    }

    @AssertTrue(message = "Les exemplaires disponibles ne peuvent pas dépasser le nombre total.")
    public boolean isDisponiblesCoherents() {
        return exemplairesDisponibles <= exemplairesTotal;
    }

    // ===== Getters / setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre == null ? null : titre.strip(); }

    public String getAuteur() { return auteur; }
    public void setAuteur(String auteur) { this.auteur = auteur == null ? null : auteur.strip(); }

    public int getAnneePublication() { return anneePublication; }
    public void setAnneePublication(int anneePublication) { this.anneePublication = anneePublication; }

    public int getExemplairesTotal() { return exemplairesTotal; }
    public void setExemplairesTotal(int exemplairesTotal) { this.exemplairesTotal = exemplairesTotal; }

    public int getExemplairesDisponibles() { return exemplairesDisponibles; }
    public void setExemplairesDisponibles(int exemplairesDisponibles) {
        this.exemplairesDisponibles = exemplairesDisponibles;
    }

    public List<Emprunt> getEmprunts() { return emprunts; }
}