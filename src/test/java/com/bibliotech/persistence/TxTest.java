package com.bibliotech.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bibliotech.model.Livre;
import com.bibliotech.support.BaseDeTestH2;
import org.junit.jupiter.api.Test;

class TxTest extends BaseDeTestH2 {

    @Test
    void une_exception_annule_toutes_les_modifications_rollback() {
        Long id = creerLivre("Clean Code", "Martin", 3).getId();

        assertThatThrownBy(() -> Tx.inTransaction(em -> {
            Livre livre = em.find(Livre.class, id);
            livre.setExemplairesDisponibles(0);
            livre.setTitre("Titre modifié");
            em.flush();                       // l'UPDATE est réellement envoyé à la base...
            return panneSimulee();            // ...puis tout échoue
        })).isInstanceOf(IllegalStateException.class);

        Livre apres = livreRepository.findById(id).orElseThrow();
        assertThat(apres.getExemplairesDisponibles()).isEqualTo(3);     // ROLLBACK : rien n'a changé
        assertThat(apres.getTitre()).isEqualTo("Clean Code");
    }

    private static Object panneSimulee() {
        throw new IllegalStateException("Panne simulée après l'UPDATE");
    }
}