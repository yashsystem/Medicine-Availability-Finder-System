package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;
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

@WebServlet("/owner-login")
public class OwnerLoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null
                || email.trim().isEmpty()
                || password.isEmpty() || password.length() > 72) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Please enter a valid email and password.");
            return;
        }

        String sql =
                "SELECT id, owner_name, email, password_hash, pharmacy_id "
                + "FROM pharmacy_owners WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()
                        || !BCrypt.checkpw(
                                password, rs.getString("password_hash"))) {
                    response.setStatus(
                            HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().println(
                            "<h2>Invalid email or password.</h2>"
                            + "<p><a href='owner-login.html'>Try again</a></p>");
                    return;
                }

                // Create a fresh session after successful authentication.
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }

                HttpSession session = request.getSession(true);
                session.setAttribute("ownerId", rs.getInt("id"));
                session.setAttribute("ownerName", rs.getString("owner_name"));
                session.setAttribute("pharmacyId", rs.getInt("pharmacy_id"));
                session.setMaxInactiveInterval(20 * 60);

                response.sendRedirect(
                        request.getContextPath() + "/owner-dashboard");
            }

        } catch (Exception e) {
            getServletContext().log("Owner login failed", e);
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Login failed due to a server error.");
        }
    }
}