package com.bibliotech.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    static {
        try {
            // Chargement explicite du driver (nécessaire sous Tomcat)
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConnection() {
        // Classe utilitaire : pas d'instanciation
    }

    /** Retourne une nouvelle connexion JDBC vers PostgreSQL. */
    public static Connection get() throws SQLException {
        String url  = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pwd  = System.getenv("DB_PASSWORD");

        if (url == null || user == null || pwd == null) {
            throw new IllegalStateException(
                "Variables d'environnement DB_URL / DB_USER / DB_PASSWORD non définies");
        }
        return DriverManager.getConnection(url, user, pwd);
    }
}