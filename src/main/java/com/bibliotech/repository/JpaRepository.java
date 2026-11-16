package com.bibliotech.repository;

import java.util.List;
import java.util.Optional;

/** Contrat générique (successeur du GenericDAO du TP 1). */
public interface JpaRepository<T, ID> {
    List<T>     findAll();
    Optional<T> findById(ID id);
    T           save(T entity);    // INSERT si id null, UPDATE sinon
    void        deleteById(ID id);
}