package com.bibliotech.web;

import com.bibliotech.model.Livre;
import com.bibliotech.persistence.JpaUtil;
import com.bibliotech.service.LivreService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Exercice 7 : démonstration des pièges JPA. Page de diagnostic, pas une page de l'application.
 *  GET /diagnostic/lazy?id=2        provoque la LazyInitializationException
 *  GET /diagnostic/join-fetch?id=2  correction 1 : JOIN FETCH
 *  GET /diagnostic/osiv?id=2        correction 2 : Open Session In View (anti-pattern, voir README)
 */
@WebServlet("/diagnostic/*")
public class DiagnosticServlet extends HttpServlet {

    private final LivreService livreService = new LivreService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        Long id = Long.valueOf(req.getParameter("id") == null ? "2" : req.getParameter("id"));
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        try {
            switch (path) {
                case "/lazy" -> {
                    // Le service renvoie un Livre "détaché" : son EntityManager est déjà FERMÉ
                    Livre livre = livreService.chercher(id).orElseThrow();
                    out.println("Livre chargé : " + livre.getTitre());
                    out.println("On accède à livre.getEmprunts().size() HORS de l'EntityManager...");
                    int n = livre.getEmprunts().size();               // ← LazyInitializationException
                    out.println("Nombre d'emprunts : " + n);
                }
                case "/join-fetch" -> {
                    // Correction 1 : les emprunts sont chargés DANS la requête JPQL
                    Livre livre = livreService.chercherAvecEmprunts(id).orElseThrow();
                    out.println("Correction 1 : JOIN FETCH");
                    out.println("Livre : " + livre.getTitre());
                    out.println("Nombre d'emprunts : " + livre.getEmprunts().size());
                }
                case "/osiv" -> {
                    // Correction 2 : l'EntityManager reste ouvert pendant TOUT le traitement de la requête.
                    // Remarque : la servlet manipule JPA, exactement ce que l'architecture en couches interdit.
                    try (EntityManager em = JpaUtil.newEntityManager()) {
                        Livre livre = em.find(Livre.class, id);
                        out.println("Correction 2 : Open Session In View");
                        out.println("Livre : " + livre.getTitre());
                        out.println("Nombre d'emprunts : " + livre.getEmprunts().size());   // chargé à la demande
                    }
                }
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (RuntimeException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println();
            out.println("ERREUR : " + e.getClass().getName());
            out.println(e.getMessage());
            log("Diagnostic " + path, e);                            // trace complète dans la console
        }
    }
}