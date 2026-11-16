package com.bibliotech.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.Etudiant;
import com.bibliotech.model.Livre;
import com.bibliotech.model.StatutEmprunt;
import com.bibliotech.support.BaseDeTestH2;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests des requêtes JPQL de EmpruntRepository (et de la recherche de livres). */
class EmpruntRepositoryH2Test extends BaseDeTestH2 {

    private final LocalDate aujourdhui = LocalDate.now();
    private Livre livre;
    private Etudiant alice;
    private Etudiant bob;

    @BeforeEach
    void donnees() {
        livre = creerLivre("Clean Code", "Robert C. Martin", 5);
        alice = creerEtudiant("ET123456", "Martin");
        bob   = creerEtudiant("ET234567", "Durand");

        // Alice : 1 emprunt en retard de 5 jours + 1 en cours ; Bob : 1 rendu en retard
        empruntRepository.save(new Emprunt(livre, alice, aujourdhui.minusDays(19), aujourdhui.minusDays(5)));
        empruntRepository.save(new Emprunt(livre, alice, aujourdhui.minusDays(2), aujourdhui.plusDays(12)));
        Emprunt rendu = new Emprunt(livre, bob, aujourdhui.minusDays(30), aujourdhui.minusDays(16));
        rendu.setDateRetourEffective(aujourdhui.minusDays(13));
        rendu.setStatut(Emprunt.Statut.RENDU);
        empruntRepository.save(rendu);
    }

    @Test
    void findEnRetard_ne_renvoie_que_les_emprunts_en_retard_non_rendus() {
        List<Emprunt> enRetard = empruntRepository.findEnRetard();

        assertThat(enRetard).hasSize(1);
        Emprunt e = enRetard.get(0);
        assertThat(e.getDateRetourPrevue()).isEqualTo(aujourdhui.minusDays(5));
        // JOIN FETCH : le livre est lisible alors que l'EntityManager est déjà fermé
        assertThat(e.getLivre().getTitre()).isEqualTo("Clean Code");
        // Le statut "riche" du TP 1 est recalculé : 5 jours de retard
        assertThat(e.statutMetier()).isInstanceOfSatisfying(StatutEmprunt.EnRetard.class,
                r -> assertThat(r.joursDeRetard()).isEqualTo(5));
    }

    @Test
    void findEnCoursPourEtudiant_renvoie_les_emprunts_non_rendus_de_cet_etudiant() {
        assertThat(empruntRepository.findEnCoursPourEtudiant(alice.getId())).hasSize(2);
        assertThat(empruntRepository.findEnCoursPourEtudiant(bob.getId())).isEmpty();
    }

    @Test
    void rechercher_un_livre_par_auteur_sans_tenir_compte_de_la_casse() {
        creerLivre("Le Petit Prince", "Antoine de Saint-Exupéry", 2);

        assertThat(livreRepository.rechercher("MARTIN"))
                .extracting(Livre::getTitre).containsExactly("Clean Code");
        assertThat(livreRepository.rechercher("petit"))
                .extracting(Livre::getTitre).containsExactly("Le Petit Prince");
    }
}