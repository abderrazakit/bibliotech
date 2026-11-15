package com.bibliotech.dao;

import com.bibliotech.model.Etudiant;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Accès JDBC en lecture seule à la table "etudiant". */
public class EtudiantDAO {

    private static final String SQL_FIND_ALL =
            "SELECT id, numero_etudiant, nom, prenom, email FROM etudiant ORDER BY nom, prenom";

    public List<Etudiant> findAll() {
        List<Etudiant> etudiants = new ArrayList<>();
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                etudiants.add(new Etudiant(
                        rs.getLong("id"),
                        rs.getString("numero_etudiant"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email")));
            }
            return etudiants;
        } catch (SQLException e) {
            throw new DAOException("Impossible de lister les étudiants", e);
        }
    }
}