package com.bibliotech.web;

import com.bibliotech.dao.GenericDAO;
import com.bibliotech.dao.DAOException;
import com.bibliotech.dao.LivreDAO;
import com.bibliotech.model.Livre;
import com.bibliotech.model.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur des livres.
 *  GET  /livres               liste
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

    /** Noms des champs du formulaire (= attributs "name" des <input>). */
    private static final List<String> CHAMPS = List.of(
            "titre", "auteur", "anneePublication", "exemplairesTotal", "exemplairesDisponibles");

    private GenericDAO<Livre, Long> livreDAO;

    @Override
    public void init() {
        // Partie B : implémentation en mémoire. Partie C : new LivreDAO() (JDBC).
        livreDAO = new LivreDAO();   // Partie C : persistance PostgreSQL (JDBC)
    }

    // ======================= ROUTAGE GET =======================
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getPathInfo();   // ex. null, "/", "/nouveau", "/3", "/3/edit"

        if (path == null || path.equals("/")) {
            afficherListe(req, resp);                                // GET /livres
            return;
        }
        if (path.equals("/nouveau")) {
            afficherFormulaireCreation(req, resp);                   // GET /livres/nouveau
            return;
        }

        String[] morceaux = path.substring(1).split("/");   // "3/edit" -> ["3", "edit"]
        Long id = lireId(morceaux[0]);
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (morceaux.length == 1) {
            afficherDetail(id, req, resp);                           // GET /livres/{id}
        } else if (morceaux.length == 2 && morceaux[1].equals("edit")) {
            afficherFormulaireModification(id, req, resp);           // GET /livres/{id}/edit
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ======================= ROUTAGE POST =======================
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");   // accents des formulaires
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            creerLivre(req, resp);                                   // POST /livres
            return;
        }

        String[] morceaux = path.substring(1).split("/");
        Long id = lireId(morceaux[0]);
        if (id == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (morceaux.length == 1) {
            modifierLivre(id, req, resp);                            // POST /livres/{id}
        } else if (morceaux.length == 2 && morceaux[1].equals("delete")) {
            supprimerLivre(id, req, resp);                           // POST /livres/{id}/delete
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ======================= ACTIONS =======================

    /** GET /livres (?q=texte) : liste des livres, filtrée par titre si une recherche est saisie. */
    private void afficherListe(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<Livre> livres = livreDAO.findAll();
        String q = req.getParameter("q");
        if (q != null && !q.isBlank()) {
            String recherche = q.strip().toLowerCase(Locale.ROOT);
            livres = livres.stream()
                    .filter(l -> l.titre().toLowerCase(Locale.ROOT).contains(recherche))
                    .toList();
        }
        req.setAttribute("livres", livres);
        req.getRequestDispatcher(VUE_LISTE).forward(req, resp);
    }
    /** GET /livres/{id} : fiche détaillée d'un livre (404 s'il n'existe pas). */
    private void afficherDetail(Long id, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Optional<Livre> livre = livreDAO.findById(id);
        if (livre.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Livre introuvable");
            return;
        }
        req.setAttribute("livre", livre.get());
        req.getRequestDispatcher(VUE_DETAIL).forward(req, resp);
    }

    /** GET /livres/nouveau : formulaire de création, vide. */
    private void afficherFormulaireCreation(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        afficherFormulaire("Ajouter un livre", "/livres", new HashMap<>(), new HashMap<>(), req, resp);
    }

    /** POST /livres : création d'un livre, avec validation et Post-Redirect-Get. */
    private void creerLivre(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Map<String, String> valeurs = lireFormulaire(req);
        try {
            Livre livre = construireLivre(0, valeurs);   // le Record valide les données
            livreDAO.save(livre);
            resp.sendRedirect(req.getContextPath() + "/livres");   // PRG : 302
        } catch (IllegalArgumentException e) {
            afficherFormulaire("Ajouter un livre", "/livres", valeurs, extraireErreurs(e), req, resp);
        }
    }

    /** GET /livres/{id}/edit : formulaire pré-rempli avec les valeurs du livre. */
    private void afficherFormulaireModification(Long id, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Optional<Livre> livre = livreDAO.findById(id);
        if (livre.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Livre introuvable");
            return;
        }
        afficherFormulaire("Modifier un livre", "/livres/" + id, versFormulaire(livre.get()),
                new HashMap<>(), req, resp);
    }

    /** POST /livres/{id} : mise à jour d'un livre, avec validation et Post-Redirect-Get. */
    private void modifierLivre(Long id, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (livreDAO.findById(id).isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Livre introuvable");
            return;
        }
        Map<String, String> valeurs = lireFormulaire(req);
        try {
            Livre livre = construireLivre(id, valeurs);
            livreDAO.update(id, livre);
            resp.sendRedirect(req.getContextPath() + "/livres");   // PRG : 302
        } catch (IllegalArgumentException e) {
            afficherFormulaire("Modifier un livre", "/livres/" + id, valeurs, extraireErreurs(e), req, resp);
        }
    }

    /** POST /livres/{id}/delete : suppression, puis Post-Redirect-Get. */
    private void supprimerLivre(Long id, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            livreDAO.delete(id);   // sans effet si le livre n'existe plus
        } catch (DAOException e) {
            // Clé étrangère : le livre a un historique d'emprunts
            resp.sendError(HttpServletResponse.SC_CONFLICT,
                    "Impossible de supprimer ce livre : il fait l'objet d'emprunts.");
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/livres");       // PRG : 302
    }

    // ======================= OUTILS =======================

    /** Prépare et affiche form.jsp (utilisé pour la création ET la modification). */
    private void afficherFormulaire(String titrePage, String actionUrl,
                                    Map<String, String> valeurs, Map<String, String> erreurs,
                                    HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("titrePage", titrePage);
        req.setAttribute("actionUrl", actionUrl);
        req.setAttribute("form", valeurs);
        req.setAttribute("errors", erreurs);
        req.getRequestDispatcher(VUE_FORM).forward(req, resp);
    }

    /** Convertit un Livre en valeurs texte pour pré-remplir le formulaire. */
    private Map<String, String> versFormulaire(Livre livre) {
        Map<String, String> valeurs = new HashMap<>();
        valeurs.put("titre", livre.titre());
        valeurs.put("auteur", livre.auteur());
        valeurs.put("anneePublication", String.valueOf(livre.anneePublication()));
        valeurs.put("exemplairesTotal", String.valueOf(livre.exemplairesTotal()));
        valeurs.put("exemplairesDisponibles", String.valueOf(livre.exemplairesDisponibles()));
        return valeurs;
    }

    /** Transforme l'exception en Map champ -> message pour la JSP. */
    private Map<String, String> extraireErreurs(IllegalArgumentException e) {
        if (e instanceof ValidationException ve) {
            return ve.getErreurs();                 // erreurs champ par champ
        }
        return Map.of("global", e.getMessage());    // erreur non rattachée à un champ
    }

    /** Lit les champs du formulaire tels que saisis (textes bruts, jamais null). */
    private Map<String, String> lireFormulaire(HttpServletRequest req) {
        Map<String, String> valeurs = new HashMap<>();
        for (String champ : CHAMPS) {
            String valeur = req.getParameter(champ);
            valeurs.put(champ, valeur == null ? "" : valeur);
        }
        return valeurs;
    }

    /**
     * Construit un Livre à partir des textes saisis.
     * Lève une ValidationException contenant TOUTES les erreurs (conversion + règles du Record).
     */
    private Livre construireLivre(long id, Map<String, String> v) {
        Map<String, String> erreurs = new LinkedHashMap<>();

        // 1) Conversion texte -> nombre (erreurs de format)
        Integer annee = lireEntier(v, "anneePublication", "L'année de publication", erreurs);
        Integer total = lireEntier(v, "exemplairesTotal", "Le nombre total d'exemplaires", erreurs);
        Integer dispo = lireEntier(v, "exemplairesDisponibles", "Le nombre d'exemplaires disponibles", erreurs);

        // 2) Règles métier du Record. Pour un champ non numérique, on passe une valeur
        //    "neutre" qui ne déclenche aucune règle : le Record peut ainsi valider les AUTRES champs.
        try {
            Livre livre = new Livre(id, v.get("titre"), v.get("auteur"),
                    annee != null ? annee : Livre.ANNEE_MIN,
                    total != null ? total : Integer.MAX_VALUE,
                    dispo != null ? dispo : 0);
            if (erreurs.isEmpty()) {
                return livre;
            }
        } catch (ValidationException e) {
            e.getErreurs().forEach(erreurs::putIfAbsent);   // les erreurs de format restent prioritaires
        }
        throw new ValidationException(erreurs);
    }

    /** Convertit un champ en entier ; en cas d'échec, enregistre une erreur et renvoie null. */
    private Integer lireEntier(Map<String, String> v, String champ, String libelle,
                               Map<String, String> erreurs) {
        try {
            return Integer.valueOf(v.get(champ).strip());
        } catch (NumberFormatException e) {
            erreurs.put(champ, libelle + " doit être un nombre entier.");
            return null;
        }
    }

    /** Convertit "3" en 3L ; renvoie null si ce n'est pas un nombre valide. */
    private Long lireId(String texte) {
        try {
            return Long.valueOf(texte);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}