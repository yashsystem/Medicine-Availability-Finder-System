package com.medicinefinder.database;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class CreateAdminAccount {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter admin username: ");
        String username = sc.nextLine().trim();

        System.out.print("Enter admin password: ");
        String password = sc.nextLine();

        if (username.isEmpty() || password.length() < 8) {
            System.out.println(
                    "Username is required and password must be at least 8 characters.");
            sc.close();
            return;
        }

        String checkSql =
                "SELECT COUNT(*) FROM admin_users";

        String insertSql =
                "INSERT INTO admin_users (username, password_hash) " +
                "VALUES (?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement check =
                     con.prepareStatement(checkSql)) {

            try (ResultSet rs = check.executeQuery()) {
                rs.next();

                if (rs.getInt(1) > 0) {
                    System.out.println(
                            "An admin account already exists. No account created.");
                    return;
                }
            }

            String passwordHash =
                    BCrypt.hashpw(password, BCrypt.gensalt(12));

            try (PreparedStatement insert =
                         con.prepareStatement(insertSql)) {

                insert.setString(1, username);
                insert.setString(2, passwordHash);

                insert.executeUpdate();

                System.out.println(
                        "Admin account created successfully.");
                System.out.println(
                        "Remember your username and password.");
            }

        } catch (Exception e) {
            System.out.println(
                    "Could not create admin account. Check the error below.");
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }
}