package com.bibliotech.dao;

import com.bibliotech.model.Livre;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implémentation TEMPORAIRE en mémoire, utilisée pendant la partie B.
 * Elle sera remplacée par LivreDAO (JDBC / PostgreSQL) en partie C.
 * Les données sont perdues à chaque redémarrage de Tomcat.
 */
public class LivreDAOMemoire implements GenericDAO<Livre, Long> {

    // Structures thread-safe : la servlet est appelée par plusieurs threads en parallèle
    private final Map<Long, Livre> livres = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public LivreDAOMemoire() {
        // 5 livres de démonstration
        save(new Livre(0, "Effective Java", "Joshua Bloch", 2018, 3, 3));
        save(new Livre(0, "Clean Code", "Robert C. Martin", 2008, 2, 1));
        save(new Livre(0, "Design Patterns", "Erich Gamma", 1994, 2, 2));
        save(new Livre(0, "Le Petit Prince", "Antoine de Saint-Exupéry", 1943, 4, 4));
        save(new Livre(0, "Les Misérables", "Victor Hugo", 1862, 1, 0));
    }

    @Override
    public List<Livre> findAll() {
        List<Livre> resultat = new ArrayList<>(livres.values());
        resultat.sort(Comparator.comparing(Livre::titre));   // tri par titre
        return resultat;
    }

    @Override
    public Optional<Livre> findById(Long id) {
        return Optional.ofNullable(livres.get(id));
    }

    @Override
    public void save(Livre livre) {
        long id = sequence.incrementAndGet();   // génère un nouvel identifiant
        // Le record est immuable : on crée une copie avec le nouvel id
        livres.put(id, new Livre(id, livre.titre(), livre.auteur(), livre.anneePublication(),
                livre.exemplairesTotal(), livre.exemplairesDisponibles()));
    }

    @Override
    public void update(Long id, Livre livre) {
        livres.put(id, new Livre(id, livre.titre(), livre.auteur(), livre.anneePublication(),
                livre.exemplairesTotal(), livre.exemplairesDisponibles()));
    }

    @Override
    public void delete(Long id) {
        livres.remove(id);
    }
}