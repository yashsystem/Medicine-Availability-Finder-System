package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/customer-register")
public class CustomerRegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String fullName = request.getParameter("full_name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try (PrintWriter out = response.getWriter()) {

            if (fullName == null || email == null || password == null
                    || fullName.trim().isEmpty()
                    || email.trim().isEmpty()
                    || password.isEmpty()) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST);

                out.println("<h3>Please fill in all fields.</h3>");
                out.println("<a href='customer-register.html'>Go Back</a>");
                return;
            }

            fullName = fullName.trim();
            email = email.trim().toLowerCase();

            if (fullName.length() > 100
                    || email.length() > 150
                    || password.length() < 8) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST);

                out.println("<h3>Invalid input.</h3>");
                out.println("<p>Name must be at most 100 characters, "
                        + "email at most 150 characters, and password "
                        + "at least 8 characters.</p>");
                out.println("<a href='customer-register.html'>Go Back</a>");
                return;
            }

            String hashedPassword = BCrypt.hashpw(
                    password, BCrypt.gensalt(12));

            String sql = "INSERT INTO users "
                    + "(full_name, email, password_hash) "
                    + "VALUES (?, ?, ?)";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, fullName);
                ps.setString(2, email);
                ps.setString(3, hashedPassword);

                ps.executeUpdate();

                out.println("<!DOCTYPE html>");
                out.println("<html><head><title>Registration</title></head>");
                out.println("<body style='font-family:Arial;text-align:center;"
                        + "margin-top:60px;'>");
                out.println("<h2 style='color:green;'>"
                        + "Registration Successful!</h2>");
                out.println("<p>Your customer account has been created.</p>");
                out.println("<a href='customer-login.html'>"
                        + "Login to Your Account</a>");
                out.println("</body></html>");

            } catch (SQLException e) {

                if ("23000".equals(e.getSQLState())) {
                    response.setStatus(
                            HttpServletResponse.SC_CONFLICT);

                    out.println("<h3>This email is already registered.</h3>");
                    out.println("<a href='customer-register.html'>"
                            + "Try Another Email</a>");
                } else {
                    throw e;
                }
            }

        } catch (SQLException e) {
            log("Customer registration failed.", e);

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            response.getWriter().println(
                    "<h3>Registration failed due to a server error.</h3>"
                    + "<p>Please check the server logs.</p>"
                    + "<a href='customer-register.html'>Go Back</a>");
        }
    }
}