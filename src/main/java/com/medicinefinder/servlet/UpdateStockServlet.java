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

@WebServlet("/update-stock")
public class UpdateStockServlet extends HttpServlet {

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

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        // Step 1: Verify the owner session.
        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("ownerId") == null
                || session.getAttribute("pharmacyId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/owner-login.html");
            return;
        }

        // Pharmacy ID comes from the logged-in session,
        // never from the form.
        int pharmacyId =
                (Integer) session.getAttribute("pharmacyId");

        try {
            String medicineValue =
                    request.getParameter("medicineId");
            String quantityValue =
                    request.getParameter("quantity");
            String priceValue =
                    request.getParameter("price");

            if (medicineValue == null || quantityValue == null
                    || priceValue == null
                    || medicineValue.trim().isEmpty()
                    || quantityValue.trim().isEmpty()
                    || priceValue.trim().isEmpty()) {

                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "All fields are required.");
                return;
            }

            int medicineId =
                    Integer.parseInt(medicineValue.trim());
            int quantity =
                    Integer.parseInt(quantityValue.trim());
            BigDecimal price =
                    new BigDecimal(priceValue.trim());

            if (pharmacyId <= 0 || medicineId <= 0
                    || quantity < 0 || price.signum() < 0
                    || price.scale() > 2
                    || price.precision() > 10) {

                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Please enter valid values.");
                return;
            }

            String sql =
                    "UPDATE pharmacy_stock " +
                    "SET quantity = ?, price = ? " +
                    "WHERE pharmacy_id = ? AND medicine_id = ?";

            int rows;

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, quantity);
                ps.setBigDecimal(2, price);
                ps.setInt(3, pharmacyId);
                ps.setInt(4, medicineId);

                rows = ps.executeUpdate();
            }

            try (PrintWriter out = response.getWriter()) {

                out.println("<!DOCTYPE html>");
                out.println("<html lang='en'><head>");
                out.println("<meta charset='UTF-8'>");
                out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
                out.println("<title>Update Stock Result</title>");

                out.println("<style>");
                out.println("body{font-family:Arial;background:#f2f6fc;padding:25px;}");
                out.println(".box{max-width:550px;margin:auto;background:white;padding:25px;border-radius:10px;}");
                out.println("h2{color:#174a8b;}");
                out.println("a{display:inline-block;margin:10px 10px 0 0;}");
                out.println("</style></head><body>");
                out.println("<div class='box'>");

                if (rows > 0) {
                    out.println("<h2>Stock Updated Successfully!</h2>");
                    out.println("<p>Medicine ID: " + medicineId + "</p>");
                    out.println("<p>New quantity: " + quantity + "</p>");
                    out.println("<p>New price: Rs. "
                            + escapeHtml(price.toPlainString()) + "</p>");
                } else {
                    out.println("<h2>Stock Record Not Found</h2>");
                    out.println("<p>Check the Medicine ID and ensure "
                            + "this medicine belongs to your pharmacy.</p>");
                }

                out.println("<a href='update-stock.html'>Update Stock Again</a>");
                out.println("<a href='stock-dashboard'>View Stock</a>");
                out.println("<a href='owner-dashboard'>Dashboard</a>");

                out.println("</div></body></html>");
            }

        } catch (NumberFormatException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Medicine ID and quantity must be whole numbers. "
                    + "Price must be a valid number.");

        } catch (Exception e) {
            getServletContext().log("Stock update failed", e);

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to update stock. Please check the server log.");
        }
    }
}