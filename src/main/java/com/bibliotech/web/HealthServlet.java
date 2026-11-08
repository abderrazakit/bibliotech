package com.bibliotech.web;

import com.bibliotech.dao.DatabaseConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");

        // try-with-resources : Connection, PreparedStatement et ResultSet
        // sont fermés automatiquement, même en cas d'exception
        try (Connection c = DatabaseConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT version()");
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            resp.setStatus(HttpServletResponse.SC_OK);                     // 200
            resp.getWriter().print("OK · " + rs.getString(1));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            resp.getWriter().print("KO · " + e.getMessage());
        }
    }
}