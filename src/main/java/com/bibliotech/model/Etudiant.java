package com.bibliotech.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/** Entité JPA mappée sur la table "etudiant". */
@Entity
@Table(name = "etudiant")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le numéro étudiant est obligatoire.")
    @Pattern(regexp = "[A-Z]{2}\\d{6}",
             message = "Le numéro étudiant doit comporter 2 lettres majuscules suivies de 6 chiffres (ex. ET123456).")
    @Column(name = "numero_etudiant", unique = true, nullable = false, length = 8)
    private String numeroEtudiant;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")   // VARCHAR(100) en base
    @Column(nullable = false, length = 100)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
    @Column(nullable = false, length = 100)
    private String prenom;

    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "L'adresse e-mail n'est pas valide.")
    @Column(nullable = false, length = 255)
    private String email;

    /** @JsonIgnore (exercice 7.3) : même cycle possible Etudiant → emprunts → etudiant → ... */
    @JsonIgnore
    @OneToMany(mappedBy = "etudiant", fetch = FetchType.LAZY)
    private List<Emprunt> emprunts = new ArrayList<>();

    protected Etudiant() {}

    public Etudiant(String numeroEtudiant, String nom, String prenom, String email) {
        this.numeroEtudiant = numeroEtudiant;
        setNom(nom);
        setPrenom(prenom);
        setEmail(email);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroEtudiant() { return numeroEtudiant; }
    public void setNumeroEtudiant(String numeroEtudiant) { this.numeroEtudiant = numeroEtudiant; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom == null ? null : nom.strip(); }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom == null ? null : prenom.strip(); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.strip(); }

    public List<Emprunt> getEmprunts() { return emprunts; }

    /** Pratique pour l'affichage : "Alice Martin (ET123456)". */
    public String getNomComplet() {
        return prenom + " " + nom + " (" + numeroEtudiant + ")";
    }
}