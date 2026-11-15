package com.bibliotech.web;

import com.bibliotech.dao.DAOException;
import com.bibliotech.dao.EmpruntDAO;
import com.bibliotech.dao.EtudiantDAO;
import com.bibliotech.dao.GenericDAO;
import com.bibliotech.dao.LivreDAO;
import com.bibliotech.model.Emprunt;
import com.bibliotech.model.Etudiant;
import com.bibliotech.model.Livre;
import com.bibliotech.model.LivreIndisponibleException;
import com.bibliotech.model.PenaliteCalculator;
import com.bibliotech.model.StatutEmprunt;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contrôleur des emprunts.
 *  GET  /emprunts               emprunts en cours + formulaire d'emprunt
 *  POST /emprunts               emprunter un livre (transaction C.3)
 *  POST /emprunts/{id}/retour   enregistrer un retour (transaction)
 */
@WebServlet("/emprunts/*")
public class EmpruntServlet extends HttpServlet {

    private static final String VUE_LISTE = "/WEB-INF/emprunts/list.jsp";
    private static final int DUREE_EMPRUNT_JOURS = 14;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private EmpruntDAO empruntDAO;
    private GenericDAO<Livre, Long> livreDAO;
    private EtudiantDAO etudiantDAO;

    @Override
    public void init() {
        empruntDAO = new EmpruntDAO();
        livreDAO = new LivreDAO();
        etudiantDAO = new EtudiantDAO();
    }

    // ======================= ROUTAGE =======================

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            afficherEmpruntsEnCours(req, resp);                     // GET /emprunts
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        req.setCharacterEncoding("UTF-8");
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            emprunter(req, resp);                                   // POST /emprunts
            return;
        }
        String[] morceaux = path.substring(1).split("/");          // "3/retour" -> ["3", "retour"]
        Long id = lireId(morceaux[0]);
        if (id != null && morceaux.length == 2 && morceaux[1].equals("retour")) {
            enregistrerRetour(id, req, resp);                       // POST /emprunts/{id}/retour
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ======================= ACTIONS =======================

    /** GET /emprunts : emprunts en cours + données du formulaire d'emprunt. */
    private void afficherEmpruntsEnCours(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        transfererMessageFlash(req);

        // 2 requêtes seulement (pas une requête par emprunt) : on indexe livres et étudiants par id
        List<Livre> livres = livreDAO.findAll();
        List<Etudiant> etudiants = etudiantDAO.findAll();
        Map<Long, Livre> livresParId = livres.stream()
                .collect(Collectors.toMap(Livre::id, Function.identity()));
        Map<Long, Etudiant> etudiantsParId = etudiants.stream()
                .collect(Collectors.toMap(Etudiant::id, Function.identity()));

        List<LigneEmprunt> lignes = empruntDAO.findEnCours().stream()
                .map(e -> versLigne(e, livresParId, etudiantsParId))
                .toList();

        req.setAttribute("emprunts", lignes);
        req.setAttribute("livresDisponibles", livres.stream()
                .filter(l -> l.exemplairesDisponibles() > 0)
                .sorted(Comparator.comparing(Livre::titre))
                .toList());
        req.setAttribute("etudiants", etudiants);
        req.setAttribute("dureeEmprunt", DUREE_EMPRUNT_JOURS);
        req.getRequestDispatcher(VUE_LISTE).forward(req, resp);
    }

    /** POST /emprunts : appelle la transaction enregistrerEmprunt, puis PRG. */
    private void emprunter(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long livreId = Long.parseLong(req.getParameter("livreId"));
            long etudiantId = Long.parseLong(req.getParameter("etudiantId"));
            empruntDAO.enregistrerEmprunt(livreId, etudiantId, DUREE_EMPRUNT_JOURS);
            flash(req, "succes", "Emprunt enregistré (retour prévu dans " + DUREE_EMPRUNT_JOURS + " jours).");
        } catch (LivreIndisponibleException e) {
            flash(req, "erreur", "Ce livre n'a plus d'exemplaire disponible.");
        } catch (NumberFormatException e) {
            flash(req, "erreur", "Veuillez choisir un livre et un étudiant.");
        } catch (IllegalArgumentException e) {
            flash(req, "erreur", e.getMessage());
        } catch (DAOException e) {
            flash(req, "erreur", "L'emprunt n'a pas pu être enregistré.");
        }
        resp.sendRedirect(req.getContextPath() + "/emprunts");      // PRG : 302
    }

    /** POST /emprunts/{id}/retour : appelle la transaction enregistrerRetour, puis PRG. */
    private void enregistrerRetour(long id, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            empruntDAO.enregistrerRetour(id);
            flash(req, "succes", "Retour enregistré : l'exemplaire est de nouveau disponible.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash(req, "erreur", e.getMessage());
        } catch (DAOException e) {
            flash(req, "erreur", "Le retour n'a pas pu être enregistré.");
        }
        resp.sendRedirect(req.getContextPath() + "/emprunts");      // PRG : 302
    }

    // ======================= OUTILS =======================

    /** Prépare une ligne d'affichage : libellé du statut + pénalité (PenaliteCalculator). */
    private LigneEmprunt versLigne(Emprunt e, Map<Long, Livre> livres, Map<Long, Etudiant> etudiants) {
        Livre livre = livres.get(e.livreId());
        Etudiant etudiant = etudiants.get(e.etudiantId());

        String statut = switch (e.statut()) {
            case StatutEmprunt.EnCours enCours   -> "En cours";
            case StatutEmprunt.EnRetard enRetard -> "En retard (" + enRetard.joursDeRetard() + " j)";
            case StatutEmprunt.Rendu rendu       -> "Rendu";
        };
        double penalite = PenaliteCalculator.calculerPenalite(e.statut());

        return new LigneEmprunt(
                e.id(),
                livre != null ? livre.titre() : "Livre #" + e.livreId(),
                etudiant != null ? etudiant.prenom() + " " + etudiant.nom()
                                   + " (" + etudiant.numeroEtudiant() + ")"
                                 : "Étudiant #" + e.etudiantId(),
                formater(e.dateEmprunt()),
                formater(e.dateRetourPrevue()),
                statut,
                e.statut() instanceof StatutEmprunt.EnRetard,
                String.format(Locale.FRANCE, "%.2f €", penalite));
    }

    private String formater(LocalDate date) {
        return date.format(FORMAT_DATE);
    }

    /** Message « flash » : stocké en session avant la redirection, affiché une seule fois. */
    private void flash(HttpServletRequest req, String type, String message) {
        HttpSession session = req.getSession();
        session.setAttribute("flashType", type);
        session.setAttribute("flashMessage", message);
    }

    /** Déplace le message flash de la session vers la requête (il disparaît ensuite). */
    private void transfererMessageFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("flashMessage") != null) {
            req.setAttribute("flashType", session.getAttribute("flashType"));
            req.setAttribute("flashMessage", session.getAttribute("flashMessage"));
            session.removeAttribute("flashType");
            session.removeAttribute("flashMessage");
        }
    }

    private Long lireId(String texte) {
        try {
            return Long.valueOf(texte);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}