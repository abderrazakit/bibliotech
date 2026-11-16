package com.bibliotech.web;

import com.bibliotech.model.Livre;
import com.bibliotech.model.ValidationException;
import com.bibliotech.service.LivreService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur des livres : HTTP uniquement, toute la logique est dans LivreService.
 *  GET  /livres(?q=)          liste ou recherche (avec le nombre d'emprunts en cours)
 *  GET  /livres/{id}          détail
 *  GET  /livres/nouveau       formulaire de création
 *  POST /livres               création
 *  GET  /livres/{id}/edit     formulaire de modification
 *  POST /livres/{id}          mise à jour
 *  POST /livres/{id}/delete   suppression
 */
@WebServlet("/livres/*")
public class LivreServlet extends HttpServlet {

    private static final String VUE_LISTE  = "/WEB-INF/livres/list.jsp";
    private static final String VUE_DETAIL = "/WEB-INF/livres/detail.jsp";
    private static final String VUE_FORM   = "/WEB-INF/livres/form.jsp";

    private static final List<String> CHAMPS = List.of(
            "titre", "auteur", "anneePublication", "exemplairesTotal", "exemplairesDisponibles");

    private LivreService livreService;

    @Override
    public void init() {
        livreService = new LivreService();
    }

    // ======================= ROUTAGE GET =======================
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            afficherListe(req, resp);
            return;
        }
        if (path.equals("/nouveau")) {
            afficherFormulaire("Ajouter un livre", "/livres", false,
                    new HashMap<>(), new HashMap<>(), req, resp);
            return;
        }

        String[] morceaux = path.substring(1).split("/");
        Long id = lireId(morceaux[0]);
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (morceaux.length == 1) {
            afficherDetail(id, req, resp);
        } else if (morceaux.length == 2 && morceaux[1].equals("edit")) {
            afficherFormulaireModification(id, req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ======================= ROUTAGE POST =======================
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            creerLivre(req, resp);
            return;
        }

        String[] morceaux = path.substring(1).split("/");
        Long id = lireId(morceaux[0]);
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (morceaux.length == 1) {
            modifierLivre(id, req, resp);
        } else if (morceaux.length == 2 && morceaux[1].equals("delete")) {
            supprimerLivre(id, req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ======================= ACTIONS =======================

    /** GET /livres : livres + nombre d'emprunts en cours, en UNE requête (correction du n+1). */
    private void afficherListe(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("livres", livreService.listerAvecEmpruntsEnCours(req.getParameter("q")));
        req.getRequestDispatcher(VUE_LISTE).forward(req, resp);
    }

    private void afficherDetail(Long id, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Optional<Livre> livre = livreService.chercher(id);
        if (livre.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Livre introuvable");
            return;
        }
        req.setAttribute("livre", livre.get());
        req.getRequestDispatcher(VUE_DETAIL).forward(req, resp);
    }

    private void creerLivre(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Map<String, String> valeurs = lireFormulaire(req);
        try {
            livreService.creer(construireLivre(valeurs, true));
            resp.sendRedirect(req.getContextPath() + "/livres");          // PRG
        } catch (ValidationException e) {
            afficherFormulaire("Ajouter un livre", "/livres", false, valeurs, e.getErreurs(), req, resp);
        }
    }

    private void afficherFormulaireModification(Long id, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Optional<Livre> livre = livreService.chercher(id);
        if (livre.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Livre introuvable");
            return;
        }
        afficherFormulaire("Modifier un livre", "/livres/" + id, true,
                versFormulaire(livre.get()), new HashMap<>(), req, resp);
    }

    private void modifierLivre(Long id, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Map<String, String> valeurs = lireFormulaire(req);
        try {
            livreService.modifier(id, construireLivre(valeurs, false));
            resp.sendRedirect(req.getContextPath() + "/livres");          // PRG
        } catch (ValidationException e) {                                  // à attraper AVANT IllegalArgumentException
            afficherFormulaire("Modifier un livre", "/livres/" + id, true, valeurs, e.getErreurs(), req, resp);
        } catch (IllegalArgumentException e) {                             // livre introuvable
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Livre introuvable");
        }
    }

    private void supprimerLivre(Long id, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            livreService.supprimer(id);
        } catch (IllegalStateException e) {                                // historique d'emprunts
            resp.sendError(HttpServletResponse.SC_CONFLICT, e.getMessage());
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/livres");              // PRG
    }

    // ======================= OUTILS =======================

    private void afficherFormulaire(String titrePage, String actionUrl, boolean modeEdition,
                                    Map<String, String> valeurs, Map<String, String> erreurs,
                                    HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("titrePage", titrePage);
        req.setAttribute("actionUrl", actionUrl);
        req.setAttribute("modeEdition", modeEdition);
        req.setAttribute("form", valeurs);
        req.setAttribute("errors", erreurs);
        req.getRequestDispatcher(VUE_FORM).forward(req, resp);
    }

    private Map<String, String> versFormulaire(Livre livre) {
        Map<String, String> valeurs = new HashMap<>();
        valeurs.put("titre", livre.getTitre());
        valeurs.put("auteur", livre.getAuteur());
        valeurs.put("anneePublication", String.valueOf(livre.getAnneePublication()));
        valeurs.put("exemplairesTotal", String.valueOf(livre.getExemplairesTotal()));
        valeurs.put("exemplairesDisponibles", String.valueOf(livre.getExemplairesDisponibles()));
        return valeurs;
    }

    private Map<String, String> lireFormulaire(HttpServletRequest req) {
        Map<String, String> valeurs = new HashMap<>();
        for (String champ : CHAMPS) {
            String valeur = req.getParameter(champ);
            valeurs.put(champ, valeur == null ? "" : valeur);
        }
        return valeurs;
    }

    /**
     * Convertit les textes du formulaire en Livre.
     * Erreurs de format (ex. "abc" dans l'année) + règles Bean Validation : tout est réuni
     * dans UNE ValidationException, pour afficher toutes les erreurs en une fois.
     */
    private Livre construireLivre(Map<String, String> v, boolean creation) {
        Map<String, String> erreurs = new LinkedHashMap<>();

        Integer annee = lireEntier(v, "anneePublication", "L'année de publication", erreurs);
        Integer total = lireEntier(v, "exemplairesTotal", "Le nombre total d'exemplaires", erreurs);
        Integer dispo = creation
                ? total                                                   // le service fixera dispo = total
                : lireEntier(v, "exemplairesDisponibles", "Le nombre d'exemplaires disponibles", erreurs);

        // Valeurs "neutres" pour un champ illisible : les AUTRES champs peuvent quand même être validés
        Livre livre = new Livre(v.get("titre"), v.get("auteur"),
                annee != null ? annee : Livre.ANNEE_MIN,
                total != null ? total : Integer.MAX_VALUE,
                dispo != null ? dispo : 0);

        if (!erreurs.isEmpty()) {
            try {
                livreService.verifier(livre);
            } catch (ValidationException e) {
                e.getErreurs().forEach(erreurs::putIfAbsent);             // erreurs de format prioritaires
            }
            throw new ValidationException(erreurs);
        }
        return livre;
    }

    private Integer lireEntier(Map<String, String> v, String champ, String libelle,
                               Map<String, String> erreurs) {
        try {
            return Integer.valueOf(v.get(champ).strip());
        } catch (NumberFormatException e) {
            erreurs.put(champ, libelle + " doit être un nombre entier.");
            return null;
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