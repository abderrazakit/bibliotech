package com.bibliotech.dao;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.StatutEmprunt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès JDBC à la table "emprunt". Uniquement des PreparedStatement + try-with-resources. */
public class EmpruntDAO implements GenericDAO<Emprunt, Long> {

    private static final String COLONNES =
            "id, livre_id, etudiant_id, date_emprunt, date_retour_prevue, date_retour_effective";

    private static final String SQL_FIND_ALL =
            "SELECT " + COLONNES + " FROM emprunt ORDER BY date_emprunt DESC, id DESC";

    private static final String SQL_FIND_BY_ID =
            "SELECT " + COLONNES + " FROM emprunt WHERE id = ?";

    /** Emprunts non rendus (en cours ou en retard). */
    private static final String SQL_FIND_EN_COURS =
            "SELECT " + COLONNES + " FROM emprunt "
          + "WHERE date_retour_effective IS NULL ORDER BY date_retour_prevue";

    /** Date de retour prévue dépassée ET livre non rendu. */
    private static final String SQL_FIND_EN_RETARD =
            "SELECT " + COLONNES + " FROM emprunt "
          + "WHERE date_retour_prevue < CURRENT_DATE AND date_retour_effective IS NULL "
          + "ORDER BY date_retour_prevue";

    private static final String SQL_INSERT =
            "INSERT INTO emprunt (livre_id, etudiant_id, date_emprunt, date_retour_prevue, "
          + "date_retour_effective, statut) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE emprunt SET livre_id = ?, etudiant_id = ?, date_emprunt = ?, date_retour_prevue = ?, "
          + "date_retour_effective = ?, statut = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM emprunt WHERE id = ?";

    // ======================= CRUD (GenericDAO) =======================

    @Override
    public List<Emprunt> findAll() {
        return lister(SQL_FIND_ALL, "Impossible de lister les emprunts");
    }

    @Override
    public Optional<Emprunt> findById(Long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(versEmprunt(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Impossible de lire l'emprunt " + id, e);
        }
    }

    @Override
    public void save(Emprunt emprunt) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
            remplirParametres(ps, emprunt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Impossible d'enregistrer l'emprunt", e);
        }
    }

    @Override
    public void update(Long id, Emprunt emprunt) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_UPDATE)) {
            remplirParametres(ps, emprunt);
            ps.setLong(7, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Impossible de modifier l'emprunt " + id, e);
        }
    }

    @Override
    public void delete(Long id) {
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Impossible de supprimer l'emprunt " + id, e);
        }
    }

    // ======================= MÉTHODES SUPPLÉMENTAIRES =======================

    /** Emprunts dont la date de retour prévue est passée et la date de retour effective est null. */
    public List<Emprunt> findEnRetard() {
        return lister(SQL_FIND_EN_RETARD, "Impossible de lister les emprunts en retard");
    }

    /** Emprunts non encore rendus (en cours ou en retard) — utilisé par EmpruntServlet. */
    public List<Emprunt> findEnCours() {
        return lister(SQL_FIND_EN_COURS, "Impossible de lister les emprunts en cours");
    }

    // ======================= OUTILS =======================

    /** Exécute une requête SELECT sans paramètre et convertit chaque ligne en Emprunt. */
    private List<Emprunt> lister(String sql, String messageErreur) {
        List<Emprunt> emprunts = new ArrayList<>();
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                emprunts.add(versEmprunt(rs));
            }
            return emprunts;
        } catch (SQLException e) {
            throw new DAOException(messageErreur, e);
        }
    }

    /** Remplit les 6 paramètres communs à INSERT et UPDATE. */
    private void remplirParametres(PreparedStatement ps, Emprunt e) throws SQLException {
        ps.setLong(1, e.livreId());
        ps.setLong(2, e.etudiantId());
        ps.setObject(3, e.dateEmprunt());          // LocalDate -> DATE (JDBC 4.2)
        ps.setObject(4, e.dateRetourPrevue());
        if (e.dateRetourEffective() == null) {
            ps.setNull(5, Types.DATE);             // NULL explicite et typé
        } else {
            ps.setObject(5, e.dateRetourEffective());
        }
        ps.setString(6, versCodeStatut(e.statut()));
    }

    /** Ligne SQL -> record Emprunt, avec calcul du statut. */
    private Emprunt versEmprunt(ResultSet rs) throws SQLException {
        LocalDate dateRetourPrevue = rs.getObject("date_retour_prevue", LocalDate.class);
        LocalDate dateRetourEffective = rs.getObject("date_retour_effective", LocalDate.class);
        return new Emprunt(
                rs.getLong("id"),
                rs.getLong("livre_id"),
                rs.getLong("etudiant_id"),
                rs.getObject("date_emprunt", LocalDate.class),
                dateRetourPrevue,
                dateRetourEffective,
                calculerStatut(dateRetourPrevue, dateRetourEffective));
    }

    /** Déduit le StatutEmprunt (EnCours / EnRetard / Rendu) à partir des dates. */
    static StatutEmprunt calculerStatut(LocalDate dateRetourPrevue, LocalDate dateRetourEffective) {
        if (dateRetourEffective != null) {
            long jours = Math.max(0, ChronoUnit.DAYS.between(dateRetourPrevue, dateRetourEffective));
            return new StatutEmprunt.Rendu(dateRetourEffective, jours > 0, jours);
        }
        LocalDate aujourdhui = LocalDate.now();
        if (aujourdhui.isAfter(dateRetourPrevue)) {
            return new StatutEmprunt.EnRetard(dateRetourPrevue,
                    ChronoUnit.DAYS.between(dateRetourPrevue, aujourdhui));
        }
        return new StatutEmprunt.EnCours(dateRetourPrevue);
    }

    /** StatutEmprunt -> valeur stockée en base (switch à pattern matching exhaustif). */
    static String versCodeStatut(StatutEmprunt statut) {
        return switch (statut) {
            case StatutEmprunt.EnCours enCours   -> "EN_COURS";
            case StatutEmprunt.EnRetard enRetard -> "EN_COURS";   // le retard se calcule, il ne se stocke pas
            case StatutEmprunt.Rendu rendu       -> "RENDU";
        };
    }
}