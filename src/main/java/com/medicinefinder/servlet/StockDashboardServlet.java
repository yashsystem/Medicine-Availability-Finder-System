package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/stock-dashboard")
public class StockDashboardServlet extends HttpServlet {

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
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("ownerId") == null
                || session.getAttribute("pharmacyId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/owner-login.html");
            return;
        }

        int pharmacyId =
                (Integer) session.getAttribute("pharmacyId");

        response.setContentType("text/html;charset=UTF-8");

        String sql =
                "SELECT p.name AS pharmacy, p.city, "
                + "m.id AS medicine_id, m.name AS medicine, "
                + "m.strength, s.quantity, s.price "
                + "FROM pharmacy_stock s "
                + "JOIN pharmacies p ON p.id = s.pharmacy_id "
                + "JOIN medicines m ON m.id = s.medicine_id "
                + "WHERE p.id = ? "
                + "ORDER BY s.quantity ASC, m.name ASC";

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html><html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Stock Dashboard</title>");

            out.println("<style>");
            out.println("*{box-sizing:border-box}");
            out.println("body{font-family:Arial,sans-serif;background:#f2f6fc;margin:0;padding:22px;color:#263445}");
            out.println(".container{max-width:1150px;margin:auto;background:white;padding:24px;border-radius:14px;box-shadow:0 4px 18px #dce3ed}");
            out.println("h2{color:#174a8b;margin-top:0}");
            out.println(".subtitle{color:#64748b}");
            out.println(".cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:14px;margin:22px 0}");
            out.println(".card{padding:18px;border-radius:10px;background:#eef5ff;border:1px solid #dbeafe}");
            out.println(".card h3{font-size:14px;margin:0 0 10px;color:#475569}");
            out.println(".number{font-size:28px;font-weight:bold;color:#174a8b}");
            out.println(".warning{background:#fff7ed;color:#9a3412;border:1px solid #fed7aa;padding:14px;border-radius:9px;margin:16px 0}");
            out.println(".danger{background:#fef2f2;color:#b91c1c;border-color:#fecaca}");
            out.println(".success{background:#f0fdf4;color:#166534;border:1px solid #bbf7d0;padding:14px;border-radius:9px;margin:16px 0}");
            out.println(".table-wrap{overflow-x:auto}");
            out.println("table{width:100%;border-collapse:collapse;margin-top:15px;min-width:750px}");
            out.println("th,td{border:1px solid #e2e8f0;padding:12px;text-align:left}");
            out.println("th{background:#1769aa;color:white}");
            out.println("tr:nth-child(even){background:#f8fafc}");
            out.println("tr.low-row{background:#fff7ed}");
            out.println("tr.out-row{background:#fef2f2}");
            out.println(".low{color:#b45309;font-weight:bold}");
            out.println(".available{color:#15803d;font-weight:bold}");
            out.println(".out{color:#b91c1c;font-weight:bold}");
            out.println(".links{display:flex;flex-wrap:wrap;gap:10px;margin-top:22px}");
            out.println(".links a{display:inline-block;background:#1769aa;color:white;text-decoration:none;padding:10px 14px;border-radius:7px}");
            out.println(".links a:hover{background:#104b7c}");
            out.println("@media(max-width:600px){.container{padding:15px}.number{font-size:23px}}");
            out.println("</style></head><body><div class='container'>");

            out.println("<h2>Medicine Stock Dashboard</h2>");
            out.println("<p class='subtitle'>Monitor your pharmacy's stock and refill medicines when needed.</p>");

            int totalRecords = 0;
            int lowStockCount = 0;
            int outOfStockCount = 0;
            boolean queryFailed = false;

            StringBuilder rows = new StringBuilder();

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, pharmacyId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        totalRecords++;

                        int quantity = rs.getInt("quantity");
                        String status;
                        String statusClass;
                        String rowClass;

                        if (quantity == 0) {
                            status = "Out of Stock";
                            statusClass = "out";
                            rowClass = "out-row";
                            outOfStockCount++;
                        } else if (quantity <= 5) {
                            status = "Low Stock";
                            statusClass = "low";
                            rowClass = "low-row";
                            lowStockCount++;
                        } else {
                            status = "Available";
                            statusClass = "available";
                            rowClass = "";
                        }

                        rows.append("<tr class='")
                                .append(rowClass).append("'>");

                        rows.append("<td>")
                                .append(escapeHtml(rs.getString("pharmacy")))
                                .append("</td>");

                        rows.append("<td>")
                                .append(escapeHtml(rs.getString("city")))
                                .append("</td>");

                        rows.append("<td>")
                                .append(rs.getInt("medicine_id"))
                                .append("</td>");

                        rows.append("<td>")
                                .append(escapeHtml(rs.getString("medicine")))
                                .append("</td>");

                        rows.append("<td>")
                                .append(escapeHtml(rs.getString("strength")))
                                .append("</td>");

                        rows.append("<td><strong>")
                                .append(quantity)
                                .append("</strong></td>");

                        rows.append("<td>")
                                .append(rs.getBigDecimal("price") == null
                                        ? "Not set"
                                        : "₹" + rs.getBigDecimal("price"))
                                .append("</td>");

                        rows.append("<td class='")
                                .append(statusClass)
                                .append("'>")
                                .append(status)
                                .append("</td></tr>");
                    }
                }

            } catch (Exception e) {
                getServletContext().log(
                        "Stock dashboard query failed", e);
                queryFailed = true;
            }

            if (queryFailed) {
                out.println("<div class='warning'>Unable to load stock. Please check the server log.</div>");
            } else {
                out.println("<div class='cards'>");

                out.println("<div class='card'><h3>Total Stock Records</h3><div class='number'>"
                        + totalRecords + "</div></div>");

                out.println("<div class='card'><h3>Low Stock (1–5)</h3><div class='number'>"
                        + lowStockCount + "</div></div>");

                out.println("<div class='card'><h3>Out of Stock</h3><div class='number'>"
                        + outOfStockCount + "</div></div>");

                out.println("</div>");

                if (outOfStockCount > 0) {
                    out.println("<div class='warning danger'><strong>Action required:</strong> "
                            + outOfStockCount
                            + " medicine record(s) have zero stock. Please restock them.</div>");
                }

                if (lowStockCount > 0) {
                    out.println("<div class='warning'><strong>Low-stock alert:</strong> "
                            + lowStockCount
                            + " medicine record(s) have 1–5 units remaining.</div>");
                }

                if (lowStockCount == 0 && outOfStockCount == 0) {
                    out.println("<div class='success'>All listed medicines have more than 5 units in stock.</div>");
                }

                out.println("<h3>Your Pharmacy Inventory</h3>");
                out.println("<div class='table-wrap'><table>");
                out.println("<thead><tr>");
                out.println("<th>Pharmacy</th><th>City</th>");
                out.println("<th>Medicine ID</th><th>Medicine</th>");
                out.println("<th>Strength</th><th>Quantity</th>");
                out.println("<th>Price</th><th>Status</th>");
                out.println("</tr></thead><tbody>");

                if (totalRecords == 0) {
                    out.println("<tr><td colspan='8'>No stock records found. Add medicine stock to get started.</td></tr>");
                } else {
                    out.println(rows);
                }

                out.println("</tbody></table></div>");
            }

            out.println("<div class='links'>");
            out.println("<a href='owner-dashboard'>Owner Dashboard</a>");
            out.println("<a href='add-stock.html'>Add Medicine Stock</a>");
            out.println("<a href='update-stock.html'>Update Stock</a>");
            out.println("<a href='index.html'>Home</a>");
            out.println("</div>");

            out.println("</div></body></html>");
        }
    }
}