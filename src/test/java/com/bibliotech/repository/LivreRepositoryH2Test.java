package com.bibliotech.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.Etudiant;
import com.bibliotech.model.Livre;
import com.bibliotech.model.LivreAvecEmprunts;
import com.bibliotech.support.BaseDeTestH2;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercice 7.2 : la requête unique (LEFT JOIN + GROUP BY) qui corrige le n+1. */
class LivreRepositoryH2Test extends BaseDeTestH2 {

    @Test
    void une_seule_requete_compte_les_emprunts_en_cours_de_chaque_livre() {
        LocalDate aujourdhui = LocalDate.now();
        Livre clean = creerLivre("Clean Code", "Martin", 3);
        creerLivre("Le Petit Prince", "Saint-Exupéry", 2);
        Etudiant alice = creerEtudiant("ET123456", "Martin");

        // Clean Code : 2 en cours + 1 rendu ; Le Petit Prince : aucun
        empruntRepository.save(new Emprunt(clean, alice, aujourdhui, aujourdhui.plusDays(14)));
        empruntRepository.save(new Emprunt(clean, alice, aujourdhui, aujourdhui.plusDays(7)));
        Emprunt rendu = new Emprunt(clean, alice, aujourdhui.minusDays(20), aujourdhui.minusDays(6));
        rendu.setDateRetourEffective(aujourdhui.minusDays(8));
        rendu.setStatut(Emprunt.Statut.RENDU);
        empruntRepository.save(rendu);

        List<LivreAvecEmprunts> lignes = livreRepository.findAllAvecEmpruntsEnCours(null);

        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0).getLivre().getTitre()).isEqualTo("Clean Code");
        assertThat(lignes.get(0).getEmpruntsEnCours()).isEqualTo(2);   // le rendu n'est pas compté
        assertThat(lignes.get(1).getLivre().getTitre()).isEqualTo("Le Petit Prince");
        assertThat(lignes.get(1).getEmpruntsEnCours()).isZero();        // LEFT JOIN : présent avec 0
    }
}