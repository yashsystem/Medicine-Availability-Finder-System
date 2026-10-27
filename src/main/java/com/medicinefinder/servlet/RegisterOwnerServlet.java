package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/owner-register")
public class RegisterOwnerServlet extends HttpServlet {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private void showMessage(HttpServletResponse response,
            int status, String message) throws IOException {

        response.setStatus(status);
        response.setContentType("text/html;charset=UTF-8");

        response.getWriter().println(
                "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>Owner Registration</title></head>"
                + "<body style='font-family:Arial;background:#f2f6fc;"
                + "padding:30px;text-align:center;'>"
                + "<h2>" + escapeHtml(message) + "</h2>"
                + "<p><a href='owner-register'>Back to Registration</a></p>"
                + "<p><a href='index.html'>Home</a></p>"
                + "</body></html>");
    }

    // GET: Display registration form with pharmacies from database.
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/html;charset=UTF-8");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, name, city, address "
                     + "FROM pharmacies ORDER BY city, name");
             ResultSet rs = ps.executeQuery()) {

            StringBuilder options = new StringBuilder();
            boolean hasPharmacies = false;

            while (rs.next()) {
                hasPharmacies = true;

                int id = rs.getInt("id");
                String name = rs.getString("name");
                String city = rs.getString("city");
                String address = rs.getString("address");

                String label = name
                        + (city != null && !city.trim().isEmpty()
                        ? " - " + city : "")
                        + (address != null && !address.trim().isEmpty()
                        ? " (" + address + ")" : "");

                options.append("<option value='")
                        .append(id)
                        .append("'>")
                        .append(escapeHtml(label))
                        .append("</option>");
            }

            response.getWriter().println(
                    "<!DOCTYPE html><html lang='en'><head>"
                    + "<meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<title>Pharmacy Owner Registration</title>"
                    + "<style>"
                    + "body{font-family:Arial,sans-serif;background:#f2f6fc;"
                    + "padding:25px 15px;}"
                    + ".container{max-width:450px;margin:auto;padding:25px;"
                    + "background:white;border-radius:12px;"
                    + "box-shadow:0 3px 12px #ccc;}"
                    + "h2{text-align:center;color:#174a8b;}"
                    + "label{display:block;margin-top:15px;font-weight:bold;}"
                    + "input,select,button{width:100%;box-sizing:border-box;"
                    + "padding:11px;margin-top:6px;border:1px solid #ccc;"
                    + "border-radius:6px;}"
                    + "button{margin-top:22px;background:#1769aa;color:white;"
                    + "border:none;cursor:pointer;}"
                    + ".note{font-size:13px;color:#555;}"
                    + "</style></head><body><div class='container'>"
                    + "<h2>Pharmacy Owner Registration</h2>");

            if (!hasPharmacies) {
                response.getWriter().println(
                        "<p>No pharmacies are registered yet. "
                        + "Please contact the administrator to add a pharmacy.</p>");
            } else {
                response.getWriter().println(
                        "<form action='owner-register' method='post'>"
                        + "<label for='ownerName'>Owner Name</label>"
                        + "<input type='text' id='ownerName' name='ownerName' "
                        + "maxlength='100' required>"
                        + "<label for='email'>Email Address</label>"
                        + "<input type='email' id='email' name='email' "
                        + "maxlength='150' required>"
                        + "<label for='password'>Password</label>"
                        + "<input type='password' id='password' name='password' "
                        + "minlength='8' maxlength='72' required>"
                        + "<p class='note'>Use at least 8 characters. "
                        + "Your password is securely hashed.</p>"
                        + "<label for='pharmacyId'>Select Pharmacy</label>"
                        + "<select id='pharmacyId' name='pharmacyId' required>"
                        + "<option value=''>Choose your pharmacy</option>"
                        + options
                        + "</select>"
                        + "<button type='submit'>Register</button>"
                        + "</form>");
            }

            response.getWriter().println(
                    "<p><a href='index.html'>Back to Home</a></p>"
                    + "</div></body></html>");

        } catch (Exception e) {
            getServletContext().log(
                    "Could not load pharmacies for owner registration", e);

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println(
                    "<h2>Unable to load pharmacies.</h2>"
                    + "<p>Please check the server log.</p>"
                    + "<a href='index.html'>Back to Home</a>");
        }
    }

    // POST: Register the selected pharmacy owner.
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        request.setCharacterEncoding("UTF-8");

        String ownerName = request.getParameter("ownerName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String pharmacyValue = request.getParameter("pharmacyId");

        if (ownerName == null || email == null
                || password == null || pharmacyValue == null) {
            showMessage(response, 400, "Please fill in all fields.");
            return;
        }

        ownerName = ownerName.trim();
        email = email.trim().toLowerCase(java.util.Locale.ROOT);

        if (ownerName.isEmpty() || ownerName.length() > 100
                || email.length() > 150
                || !EMAIL_PATTERN.matcher(email).matches()
                || password.length() < 8
                || password.length() > 72) {
            showMessage(response, 400,
                    "Invalid details. Check your name, email and password.");
            return;
        }

        int pharmacyId;

        try {
            pharmacyId = Integer.parseInt(pharmacyValue);

            if (pharmacyId <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            showMessage(response, 400,
                    "Please select a valid pharmacy.");
            return;
        }

        try {
            String passwordHash =
                    BCrypt.hashpw(password, BCrypt.gensalt(12));

            String sql =
                    "INSERT INTO pharmacy_owners "
                    + "(owner_name, email, password_hash, pharmacy_id) "
                    + "SELECT ?, ?, ?, p.id FROM pharmacies p "
                    + "WHERE p.id = ? "
                    + "AND NOT EXISTS ("
                    + "SELECT 1 FROM pharmacy_owners po "
                    + "WHERE po.pharmacy_id = p.id)";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, ownerName);
                ps.setString(2, email);
                ps.setString(3, passwordHash);
                ps.setInt(4, pharmacyId);

                int rows = ps.executeUpdate();

                if (rows == 1) {
                    showMessage(response, 200,
                            "Registration successful! Your owner account has been created.");
                } else {
                    showMessage(response, 409,
                            "This pharmacy does not exist or already has an owner account.");
                }

            } catch (SQLException e) {
                if ("23000".equals(e.getSQLState())) {
                    showMessage(response, 409,
                            "This email or pharmacy already has an owner account.");
                } else {
                    throw e;
                }
            }

        } catch (Exception e) {
            getServletContext().log("Owner registration failed", e);
            showMessage(response, 500,
                    "Registration failed. Please check the server log.");
        }
    }
}