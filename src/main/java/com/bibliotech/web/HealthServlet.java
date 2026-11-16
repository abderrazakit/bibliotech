package com.bibliotech.web;

import com.bibliotech.service.SanteService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/health")
public class HealthServlet extends HttpServlet {

    private final SanteService santeService = new SanteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        try {
            String version = santeService.versionBase();
            resp.setStatus(HttpServletResponse.SC_OK);                       // 200
            resp.getWriter().print("OK · " + version);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);   // 500
            resp.getWriter().print("KO · " + e.getMessage());
        }
    }
}