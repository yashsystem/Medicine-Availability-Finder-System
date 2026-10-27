package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/add-stock")
public class AddStockServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {

            HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("ownerId") == null
                            || session.getAttribute("pharmacyId") == null) {

    response.sendRedirect(
            request.getContextPath() + "/owner-login.html");
    return;
}

int pharmacyId =
        (Integer) session.getAttribute("pharmacyId");

            String name = request.getParameter("name").trim();
            String strength = request.getParameter("strength").trim();
            String manufacturer = request.getParameter("manufacturer");
            int quantity = Integer.parseInt(
                    request.getParameter("quantity"));

            BigDecimal price = new BigDecimal(
                    request.getParameter("price"));

            if (name.isEmpty() || strength.isEmpty()
                    || quantity < 0 || price.signum() < 0) {
                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST);
                out.println("<h2>Invalid input. Please check your values.</h2>");
                return;
            }

            if (manufacturer == null) {
                manufacturer = "";
            }

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                try {
                    // Check whether the pharmacy exists.
                    try (PreparedStatement check = con.prepareStatement(
                            "SELECT id FROM pharmacies WHERE id = ?")) {

                        check.setInt(1, pharmacyId);

                        try (ResultSet rs = check.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException(
                                        "Selected pharmacy does not exist.");
                            }
                        }
                    }

                    // Find the medicine by name and strength.
                    int medicineId = -1;

                    String findSql =
                            "SELECT id FROM medicines " +
                            "WHERE LOWER(name) = LOWER(?) " +
                            "AND LOWER(strength) = LOWER(?) LIMIT 1";

                    try (PreparedStatement find =
                            con.prepareStatement(findSql)) {

                        find.setString(1, name);
                        find.setString(2, strength);

                        try (ResultSet rs = find.executeQuery()) {
                            if (rs.next()) {
                                medicineId = rs.getInt("id");
                            }
                        }
                    }

                    // Insert medicine if it does not exist.
                    if (medicineId == -1) {
                        String insertMedicine =
                                "INSERT INTO medicines " +
                                "(name, strength, manufacturer) " +
                                "VALUES (?, ?, ?)";

                        try (PreparedStatement insert =
                                con.prepareStatement(
                                        insertMedicine,
                                        PreparedStatement.RETURN_GENERATED_KEYS)) {

                            insert.setString(1, name);
                            insert.setString(2, strength);
                            insert.setString(3, manufacturer);

                            insert.executeUpdate();

                            try (ResultSet keys =
                                    insert.getGeneratedKeys()) {
                                if (keys.next()) {
                                    medicineId = keys.getInt(1);
                                } else {
                                    throw new SQLException(
                                            "Could not create medicine record.");
                                }
                            }
                        }
                    }

                    // Add new stock or update existing stock.
                    String stockSql =
                            "INSERT INTO pharmacy_stock " +
                            "(pharmacy_id, medicine_id, quantity, price) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "quantity = VALUES(quantity), " +
                            "price = VALUES(price)";

                    try (PreparedStatement stock =
                            con.prepareStatement(stockSql)) {

                        stock.setInt(1, pharmacyId);
                        stock.setInt(2, medicineId);
                        stock.setInt(3, quantity);
                        stock.setBigDecimal(4, price);

                        stock.executeUpdate();
                    }

                    con.commit();

                    out.println("<h2>Medicine stock saved successfully!</h2>");
                    out.println("<p>Medicine: "
                            + escapeHtml(name) + "</p>");
                    out.println("<p>Strength: "
                            + escapeHtml(strength) + "</p>");
                    out.println("<p>Quantity: " + quantity + "</p>");
                    out.println("<p>Price: Rs. "
                            + price.toPlainString() + "</p>");
                    out.println("<p><a href='add-stock.html'>Add another medicine</a></p>");
                    out.println("<p><a href='index.html'>Home</a></p>");

                } catch (Exception e) {
                    con.rollback();
                    throw e;
                } finally {
                    con.setAutoCommit(true);
                }

            } catch (Exception e) {
                getServletContext().log("Could not save medicine stock", e);
                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("<h2>Could not save medicine stock.</h2>");
                out.println("<p>Please check the server log.</p>");
                out.println("<a href='add-stock.html'>Go back</a>");
            }
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}