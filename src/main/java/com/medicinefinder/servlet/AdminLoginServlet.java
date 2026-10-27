package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/admin-login")
public class AdminLoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null
                || username.trim().isEmpty()
                || password.isEmpty()) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/admin-login.html?error=1");
            return;
        }

        String sql =
                "SELECT id, username, password_hash " +
                "FROM admin_users WHERE username = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    if (storedHash != null
                            && BCrypt.checkpw(password, storedHash)) {

                        HttpSession oldSession =
                                request.getSession(false);

                        if (oldSession != null) {
                            oldSession.invalidate();
                        }

                        HttpSession session =
                                request.getSession(true);

                        session.setAttribute(
                                "adminId", rs.getInt("id"));
                        session.setAttribute(
                                "adminUsername", rs.getString("username"));

                        session.setMaxInactiveInterval(20 * 60);

                        response.sendRedirect(
                                request.getContextPath()
                                + "/admin-dashboard");
                        return;
                    }
                }
            }

        } catch (Exception e) {
            log("Admin login failed due to a server error", e);
            response.sendRedirect(
                    request.getContextPath()
                    + "/admin-login.html?error=server");
            return;
        }

        response.sendRedirect(
                request.getContextPath()
                + "/admin-login.html?error=1");
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.sendRedirect(
                request.getContextPath() + "/admin-login.html");
    }
}