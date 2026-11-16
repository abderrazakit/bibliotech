package com.bibliotech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.Etudiant;
import com.bibliotech.model.Livre;
import com.bibliotech.model.ValidationException;
import java.time.LocalDate;
import java.time.Year;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Règles Bean Validation des entités (remplace ModelValidationTest du TP 1). */
class ValidationTest {

    /** Vérifie qu'une ValidationException est levée avec une erreur sur ce champ. */
    private void assertErreurSur(Object entite, String champ) {
        assertThatThrownBy(() -> Validateur.valider(entite))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErreurs()).containsKey(champ));
    }

    // ===================== Livre =====================

    @Test
    void livre_valide_est_accepte_et_normalise() {
        Livre livre = new Livre("  Effective Java ", "Joshua Bloch", 2018, 3);
        assertThatCode(() -> Validateur.valider(livre)).doesNotThrowAnyException();
        assertThat(livre.getTitre()).isEqualTo("Effective Java");     // strip() dans le setter
    }

    @Test
    void titre_vide_est_refuse() {
        assertErreurSur(new Livre("   ", "Bloch", 2018, 3), "titre");
    }

    @Test
    void annee_hors_limites_est_refusee() {
        assertErreurSur(new Livre("T", "A", 1499, 1), "anneePublication");
        assertErreurSur(new Livre("T", "A", Year.now().getValue() + 1, 1), "anneePublication");
    }

    @Test
    void disponibles_superieurs_au_total_sont_refuses() {
        assertErreurSur(new Livre("T", "A", 2000, 2, 3), "exemplairesDisponibles");
    }

    @Test
    void toutes_les_erreurs_sont_signalees_en_une_fois() {
        assertThatThrownBy(() -> Validateur.valider(new Livre("", "", 1400, 1)))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.getErreurs()).containsKeys("titre", "auteur", "anneePublication"));
    }

    // ===================== Etudiant =====================

    @ParameterizedTest
    @ValueSource(strings = {"et123456", "ET12345", "ET1234567", "E1234567", "123456ET"})
    void numero_etudiant_mal_forme_est_refuse(String numero) {
        assertErreurSur(new Etudiant(numero, "Nom", "Prenom", "a@univ.fr"), "numeroEtudiant");
    }

    @Test
    void email_invalide_est_refuse() {
        assertErreurSur(new Etudiant("ET123456", "Nom", "Prenom", "pas-un-email"), "email");
    }

    // ===================== Emprunt =====================

    @Test
    void retour_prevu_avant_l_emprunt_est_refuse() {
        LocalDate aujourdhui = LocalDate.now();
        Emprunt emprunt = new Emprunt(new Livre("T", "A", 2000, 1),
                new Etudiant("ET123456", "Nom", "Prenom", "a@univ.fr"),
                aujourdhui, aujourdhui.minusDays(1));
        assertErreurSur(emprunt, "dateRetourPrevue");
    }
}