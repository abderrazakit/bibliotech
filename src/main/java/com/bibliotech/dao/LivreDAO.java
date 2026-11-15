package com.bibliotech.dao;

import com.bibliotech.model.Livre;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès JDBC à la table "livre". Uniquement des PreparedStatement + try-with-resources. */
public class LivreDAO implements GenericDAO<Livre, Long> {

    private static final String SQL_FIND_ALL =
            "SELECT id, titre, auteur, annee_publication, exemplaires_total, exemplaires_disponibles "
          + "FROM livre ORDER BY titre";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, titre, auteur, annee_publication, exemplaires_total, exemplaires_disponibles "
          + "FROM livre WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO livre (titre, auteur, annee_publication, exemplaires_total, exemplaires_disponibles) "
          + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE livre SET titre = ?, auteur = ?, annee_publication = ?, "
          + "exemplaires_total = ?, exemplaires_disponibles = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM livre WHERE id = ?";

    @Override
    public List<Livre> findAll() {
        List<Livre> livres = new ArrayList<>();
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                livres.add(versLivre(rs));
            }
            return livres;
        } catch (SQLException e) {
            throw new DAOException("Impossible de lister les livres", e);
        }
    }

    @Override
    public Optional<Livre> findById(Long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setLong(1, id);                          // valeur liée au paramètre "?"
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(versLivre(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Impossible de lire le livre " + id, e);
        }
    }

    @Override
    public void save(Livre livre) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            ps.setString(1, livre.titre());
            ps.setString(2, livre.auteur());
            ps.setInt(3, livre.anneePublication());
            ps.setInt(4, livre.exemplairesTotal());
            ps.setInt(5, livre.exemplairesDisponibles());
            ps.executeUpdate();                         // l'id est généré par la base
        } catch (SQLException e) {
            throw new DAOException("Impossible d'enregistrer le livre", e);
        }
    }

    @Override
    public void update(Long id, Livre livre) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, livre.titre());
            ps.setString(2, livre.auteur());
            ps.setInt(3, livre.anneePublication());
            ps.setInt(4, livre.exemplairesTotal());
            ps.setInt(5, livre.exemplairesDisponibles());
            ps.setLong(6, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Impossible de modifier le livre " + id, e);
        }
    }

    @Override
    public void delete(Long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Impossible de supprimer le livre " + id, e);
        }
    }

    /** Convertit la ligne courante du ResultSet en Livre (snake_case SQL -> record Java). */
    private Livre versLivre(ResultSet rs) throws SQLException {
        return new Livre(
                rs.getLong("id"),
                rs.getString("titre"),
                rs.getString("auteur"),
                rs.getInt("annee_publication"),
                rs.getInt("exemplaires_total"),
                rs.getInt("exemplaires_disponibles"));
    }
}