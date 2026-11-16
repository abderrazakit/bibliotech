package com.bibliotech.web;

import com.bibliotech.model.Emprunt;
import com.bibliotech.model.LivreIndisponibleException;
import com.bibliotech.model.StatutEmprunt;
import com.bibliotech.service.EmpruntService;
import com.bibliotech.service.EtudiantService;
import com.bibliotech.service.LivreService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Contrôleur des emprunts : délègue tout aux services, n'ouvre jamais de transaction.
 *  GET  /emprunts               emprunts en cours + formulaire d'emprunt
 *  POST /emprunts               emprunter un livre
 *  POST /emprunts/{id}/retour   enregistrer un retour
 */
@WebServlet("/emprunts/*")
public class EmpruntServlet extends HttpServlet {

    private static final String VUE_LISTE = "/WEB-INF/emprunts/list.jsp";
    private static final int DUREE_EMPRUNT_JOURS = 14;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private EmpruntService empruntService;
    private LivreService livreService;
    private EtudiantService etudiantService;

    @Override
    public void init() {
        empruntService = new EmpruntService();
        livreService = new LivreService();
        etudiantService = new EtudiantService();
    }

    // ======================= ROUTAGE =======================

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            afficherEmpruntsEnCours(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            emprunter(req, resp);
            return;
        }
        String[] morceaux = path.substring(1).split("/");
        Long id = lireId(morceaux[0]);
        if (id != null && morceaux.length == 2 && morceaux[1].equals("retour")) {
            enregistrerRetour(id, req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ======================= ACTIONS =======================

    private void afficherEmpruntsEnCours(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        transfererMessageFlash(req);

        // findEnCours() charge livre ET étudiant (JOIN FETCH) : aucune requête par ligne
        List<LigneEmprunt> lignes = empruntService.listerEnCours().stream()
                .map(this::versLigne)
                .toList();

        req.setAttribute("emprunts", lignes);
        req.setAttribute("livresDisponibles", livreService.lister().stream()
                .filter(l -> l.getExemplairesDisponibles() > 0)
                .toList());                                     // déjà triés par titre (JPQL)
        req.setAttribute("etudiants", etudiantService.lister());
        req.setAttribute("dureeEmprunt", DUREE_EMPRUNT_JOURS);
        req.getRequestDispatcher(VUE_LISTE).forward(req, resp);
    }

    private void emprunter(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long livreId = Long.parseLong(req.getParameter("livreId"));
            long etudiantId = Long.parseLong(req.getParameter("etudiantId"));

            // Aucune transaction ici : c'est le service qui orchestre
            empruntService.enregistrerEmprunt(livreId, etudiantId, DUREE_EMPRUNT_JOURS);
            flash(req, "succes", "Emprunt enregistré (retour prévu dans " + DUREE_EMPRUNT_JOURS + " jours).");
        } catch (LivreIndisponibleException e) {
            flash(req, "erreur", "Ce livre n'a plus d'exemplaire disponible.");
        } catch (NumberFormatException e) {                    // avant IllegalArgumentException (sous-classe)
            flash(req, "erreur", "Veuillez choisir un livre et un étudiant.");
        } catch (IllegalArgumentException e) {
            flash(req, "erreur", e.getMessage());
        } catch (RuntimeException e) {                         // erreur technique (base, etc.)
            log("Échec de l'emprunt", e);
            flash(req, "erreur", "L'emprunt n'a pas pu être enregistré.");
        }
        resp.sendRedirect(req.getContextPath() + "/emprunts");  // PRG
    }

    private void enregistrerRetour(long id, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            empruntService.enregistrerRetour(id);
            flash(req, "succes", "Retour enregistré : l'exemplaire est de nouveau disponible.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash(req, "erreur", e.getMessage());
        } catch (RuntimeException e) {
            log("Échec du retour", e);
            flash(req, "erreur", "Le retour n'a pas pu être enregistré.");
        }
        resp.sendRedirect(req.getContextPath() + "/emprunts");  // PRG
    }

    // ======================= OUTILS =======================

    /** Prépare une ligne d'affichage : libellé du statut + pénalité. */
    private LigneEmprunt versLigne(Emprunt e) {
        StatutEmprunt statutMetier = e.statutMetier();

        String statut = switch (statutMetier) {
            case StatutEmprunt.EnCours enCours   -> "En cours";
            case StatutEmprunt.EnRetard enRetard -> "En retard (" + enRetard.joursDeRetard() + " j)";
            case StatutEmprunt.Rendu rendu       -> "Rendu";
        };

        return new LigneEmprunt(
                e.getId(),
                e.getLivre().getTitre(),
                e.getEtudiant().getNomComplet(),
                formater(e.getDateEmprunt()),
                formater(e.getDateRetourPrevue()),
                statut,
                statutMetier instanceof StatutEmprunt.EnRetard,
                String.format(Locale.FRANCE, "%.2f €", empruntService.penalite(e)));
    }

    private String formater(LocalDate date) {
        return date.format(FORMAT_DATE);
    }

    private void flash(HttpServletRequest req, String type, String message) {
        HttpSession session = req.getSession();
        session.setAttribute("flashType", type);
        session.setAttribute("flashMessage", message);
    }

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