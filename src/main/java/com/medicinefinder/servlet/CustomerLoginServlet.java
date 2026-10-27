package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/customer-login")
public class CustomerLoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Email and password are required.");
            return;
        }

        email = email.trim().toLowerCase();

        String sql = "SELECT id, full_name, password_hash "
                   + "FROM users WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    String storedHash = rs.getString("password_hash");

                    if (storedHash != null
                            && BCrypt.checkpw(password, storedHash)) {

                        // Create a fresh session after successful login.
                        HttpSession oldSession = request.getSession(false);

                        if (oldSession != null) {
                            oldSession.invalidate();
                        }

                        HttpSession session = request.getSession(true);

                        session.setAttribute(
                                "customerId", rs.getInt("id"));

                        session.setAttribute(
                                "customerName", rs.getString("full_name"));

                        session.setAttribute("customerEmail", email);

                        session.setMaxInactiveInterval(20 * 60);

                        response.sendRedirect(
                                request.getContextPath()
                                + "/customer-dashboard");

                        return;
                    }
                }
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            response.getWriter().println(
                    "<!DOCTYPE html><html><head>"
                    + "<title>Login Failed</title></head><body "
                    + "style='font-family:Arial;text-align:center;"
                    + "margin-top:60px;'>"
                    + "<h3>Invalid email or password.</h3>"
                    + "<a href='customer-login.html'>Try Again</a>"
                    + "</body></html>");

        } catch (Exception e) {

            log("Customer login failed.", e);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Login failed due to a server error. "
                    + "Please check the server logs.");
        }
    }
}