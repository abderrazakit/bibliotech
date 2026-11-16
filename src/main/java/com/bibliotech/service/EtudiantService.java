package com.bibliotech.service;

import com.bibliotech.model.Etudiant;
import com.bibliotech.repository.EtudiantRepository;

import java.util.List;

public class EtudiantService {

    private final EtudiantRepository etudiantRepository;

    public EtudiantService() {
        this(new EtudiantRepository());
    }

    public EtudiantService(EtudiantRepository etudiantRepository) {
        this.etudiantRepository = etudiantRepository;
    }

    public List<Etudiant> lister() {
        return etudiantRepository.findAll();
    }
}