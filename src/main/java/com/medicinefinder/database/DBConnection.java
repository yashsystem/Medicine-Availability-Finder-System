package com.medicinefinder.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/medicine_finder";

    private static final String USER = "root";

    // Yahan apna MySQL password likho
    private static final String PASSWORD = "Oracle";

    public static Connection getConnection()
        throws SQLException {

    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
        throw new SQLException(
                "MySQL JDBC driver is missing from the project.", e);
    }

    return DriverManager.getConnection(
            URL, USER, PASSWORD
    );
}

    public static void main(String[] args) {

        try (Connection connection = getConnection()) {

            System.out.println(
                    "Database connected successfully!"
            );

        } catch (SQLException e) {

            System.out.println(
                    "Database connection failed!"
            );

            e.printStackTrace();
        }
    }
}