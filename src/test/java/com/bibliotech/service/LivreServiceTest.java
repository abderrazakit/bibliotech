package com.bibliotech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bibliotech.model.Livre;
import com.bibliotech.model.ValidationException;
import com.bibliotech.repository.EmpruntRepository;
import com.bibliotech.repository.LivreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests UNITAIRES : les repositories sont des mocks, aucune base de données. */
@ExtendWith(MockitoExtension.class)
class LivreServiceTest {

    @Mock LivreRepository livreRepository;
    @Mock EmpruntRepository empruntRepository;

    @InjectMocks LivreService service;     // construit avec les 2 mocks

    @Test
    void creer_initialise_les_disponibles_au_total_puis_enregistre() {
        Livre livre = new Livre("Effective Java", "Bloch", 2018, 3, 0);   // disponibles faux exprès
        when(livreRepository.save(any(Livre.class))).thenAnswer(appel -> appel.getArgument(0));

        service.creer(livre);

        assertThat(livre.getExemplairesDisponibles()).isEqualTo(3);       // règle métier appliquée
        verify(livreRepository).save(livre);
    }

    @Test
    void creer_refuse_un_livre_invalide_sans_toucher_au_repository() {
        Livre invalide = new Livre("", "Bloch", 2018, 3);

        assertThatThrownBy(() -> service.creer(invalide))
                .isInstanceOf(ValidationException.class);

        verify(livreRepository, never()).save(any());                    // rien n'est enregistré
    }

    @Test
    void supprimer_refuse_un_livre_qui_a_des_emprunts() {
        when(empruntRepository.compterPourLivre(5L)).thenReturn(2L);

        assertThatThrownBy(() -> service.supprimer(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("emprunts");

        verify(livreRepository, never()).deleteById(anyLong());
    }

    @Test
    void supprimer_un_livre_sans_emprunt_appelle_le_repository() {
        when(empruntRepository.compterPourLivre(5L)).thenReturn(0L);

        service.supprimer(5L);

        verify(livreRepository).deleteById(5L);
    }

    @Test
    void rechercher_sans_mot_cle_renvoie_tout_le_catalogue() {
        service.rechercher("   ");

        verify(livreRepository).findAll();
        verify(livreRepository, never()).rechercher(any());
    }
}