package com.bibliotech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.Etudiant;
import com.bibliotech.model.Livre;
import com.bibliotech.model.LivreIndisponibleException;
import com.bibliotech.support.BaseDeTestH2;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests d'INTÉGRATION : vrai JPA, vraies transactions, base H2 en mémoire. */
class EmpruntServiceH2Test extends BaseDeTestH2 {

    private final EmpruntService service = new EmpruntService();
    private Livre livre;
    private Etudiant alice;
    private Etudiant bob;

    @BeforeEach
    void donnees() {                       // exécuté APRÈS viderLaBase() de la classe mère
        livre = creerLivre("Effective Java", "Bloch", 3);
        alice = creerEtudiant("ET123456", "Martin");
        bob   = creerEtudiant("ET234567", "Durand");
    }

    @Test
    void emprunter_decremente_le_stock_et_cree_un_emprunt_en_cours() {
        Long id = service.enregistrerEmprunt(livre.getId(), alice.getId(), 14);

        assertThat(stock(livre.getId())).isEqualTo(2);
        Emprunt emprunt = empruntRepository.findById(id).orElseThrow();
        assertThat(emprunt.getStatut()).isEqualTo(Emprunt.Statut.EN_COURS);
        assertThat(emprunt.getDateRetourPrevue()).isEqualTo(LocalDate.now().plusDays(14));
    }

    @Test
    void livre_sans_exemplaire_leve_LivreIndisponibleException_et_ne_change_rien() {
        Livre unique = creerLivre("Les Misérables", "Hugo", 1);
        service.enregistrerEmprunt(unique.getId(), alice.getId(), 14);       // dernier exemplaire

        assertThatThrownBy(() -> service.enregistrerEmprunt(unique.getId(), bob.getId(), 14))
                .isInstanceOf(LivreIndisponibleException.class);

        assertThat(stock(unique.getId())).isZero();
        assertThat(empruntRepository.compterPourLivre(unique.getId())).isEqualTo(1);
    }

    @Test
    void etudiant_inconnu_est_refuse_et_le_stock_ne_bouge_pas() {
        assertThatThrownBy(() -> service.enregistrerEmprunt(livre.getId(), 999_999L, 14))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(stock(livre.getId())).isEqualTo(3);
        assertThat(empruntRepository.compterPourLivre(livre.getId())).isZero();
    }

    @Test
    void duree_negative_est_refusee() {
        assertThatThrownBy(() -> service.enregistrerEmprunt(livre.getId(), alice.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retour_reincremente_le_stock_et_marque_l_emprunt_rendu() {
        Long id = service.enregistrerEmprunt(livre.getId(), alice.getId(), 14);

        service.enregistrerRetour(id);

        assertThat(stock(livre.getId())).isEqualTo(3);
        Emprunt emprunt = empruntRepository.findById(id).orElseThrow();
        assertThat(emprunt.getStatut()).isEqualTo(Emprunt.Statut.RENDU);
        assertThat(emprunt.getDateRetourEffective()).isEqualTo(LocalDate.now());
    }

    @Test
    void un_double_retour_est_refuse() {
        Long id = service.enregistrerEmprunt(livre.getId(), alice.getId(), 14);
        service.enregistrerRetour(id);

        assertThatThrownBy(() -> service.enregistrerRetour(id))
                .isInstanceOf(IllegalStateException.class);
        assertThat(stock(livre.getId())).isEqualTo(3);                      // pas de stock "en trop"
    }
}