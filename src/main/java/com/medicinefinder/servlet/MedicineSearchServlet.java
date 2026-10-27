package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/search")
public class MedicineSearchServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String medicine = request.getParameter("medicine");
        String city = request.getParameter("city");

        medicine = (medicine == null) ? "" : medicine.trim();
        city = (city == null) ? "" : city.trim();

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            out.println("<title>Medicine Search Results</title>");

            out.println("<style>");
            out.println("*{box-sizing:border-box}");
            out.println("body{font-family:Arial,sans-serif;background:#f1f5f9;"
                    + "color:#243447;margin:0;padding:20px;}");
            out.println(".container{max-width:1000px;margin:20px auto;}");
            out.println(".header{background:#0f766e;color:white;padding:24px;"
                    + "border-radius:14px;margin-bottom:20px;}");
            out.println(".header h1{margin:0 0 10px;font-size:27px;}");
            out.println(".header p{margin:6px 0;}");
            out.println(".summary{background:#ecfdf5;color:#166534;"
                    + "padding:13px 16px;border-radius:9px;margin:15px 0;}");
            out.println(".card{background:white;border:1px solid #dce5eb;"
                    + "border-radius:12px;padding:20px;margin:15px 0;"
                    + "box-shadow:0 3px 12px rgba(0,0,0,.04);}");
            out.println(".card.best{border:2px solid #16a34a;}");
            out.println(".medicine-title{margin:0 0 12px;color:#0f766e;}");
            out.println(".badge{display:inline-block;padding:6px 10px;"
                    + "border-radius:20px;font-size:12px;font-weight:bold;"
                    + "margin-bottom:10px;}");
            out.println(".best-badge{background:#dcfce7;color:#166534;}");
            out.println(".stock-badge{background:#dbeafe;color:#1d4ed8;}");
            out.println(".out-badge{background:#fee2e2;color:#991b1b;}");
            out.println(".price{font-size:24px;font-weight:bold;color:#166534;}");
            out.println(".details{line-height:1.8;overflow-wrap:anywhere;}");
            out.println(".empty{background:white;padding:22px;border-radius:10px;}");
            out.println(".back{display:inline-block;background:#0f766e;color:white;"
                    + "padding:11px 16px;text-decoration:none;border-radius:7px;"
                    + "margin-top:10px;}");
            out.println(".back:hover{background:#115e59;}");
            out.println(".search-again{background:white;padding:18px;"
                    + "border-radius:10px;margin-bottom:18px;}");
            out.println(".search-again input{padding:10px;width:100%;"
                    + "margin:5px 0 12px;border:1px solid #cbd5e1;border-radius:6px;}");
            out.println(".search-again button{background:#0f766e;color:white;"
                    + "padding:10px 15px;border:0;border-radius:6px;cursor:pointer;}");
            out.println("@media(max-width:600px){body{padding:12px}"
                    + ".header h1{font-size:22px}.card{padding:15px}}");
            out.println("</style></head><body><div class='container'>");

            out.println("<div class='header'>");
            out.println("<h1>Medicine Availability Finder</h1>");
            out.println("<p>Compare medicine prices and find pharmacies with stock.</p>");
            out.println("<p><b>Medicine:</b> " + escapeHtml(medicine) + "</p>");

            if (!city.isEmpty()) {
                out.println("<p><b>City:</b> " + escapeHtml(city) + "</p>");
            } else {
                out.println("<p><b>City:</b> All cities</p>");
            }

            out.println("</div>");

            // Search form
            out.println("<div class='search-again'>");
            out.println("<h3>Search Again</h3>");
            out.println("<form method='get' action='"
                    + request.getContextPath() + "/search'>");
            out.println("<label>Medicine name</label>");
            out.println("<input type='text' name='medicine' required value='"
                    + escapeHtml(medicine) + "' placeholder='e.g. Paracetamol'>");
            out.println("<label>City (optional)</label>");
            out.println("<input type='text' name='city' value='"
                    + escapeHtml(city) + "' placeholder='Enter city'>");
            out.println("<button type='submit'>Search Medicine</button>");
            out.println("</form></div>");

            if (medicine.isEmpty()) {

                out.println("<div class='empty'>");
                out.println("<h3>Please enter a medicine name.</h3>");
                out.println("</div>");

            } else {

                String sql =
                    "SELECT m.name, m.strength, p.name AS pharmacy, "
                    + "p.address, p.city, p.phone, s.quantity, s.price "
                    + "FROM pharmacy_stock s "
                    + "JOIN medicines m ON s.medicine_id = m.id "
                    + "JOIN pharmacies p ON s.pharmacy_id = p.id "
                    + "WHERE m.name LIKE ? "
                    + "AND (? = '' OR LOWER(p.city) LIKE LOWER(?)) "
                    + "ORDER BY CASE WHEN s.quantity > 0 THEN 0 ELSE 1 END, "
                    + "CASE WHEN s.quantity > 0 THEN s.price END ASC, "
                    + "p.city ASC, p.name ASC";

                try (Connection con = DBConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setString(1, "%" + medicine + "%");
                    ps.setString(2, city);
                    ps.setString(3, "%" + city + "%");

                    try (ResultSet rs = ps.executeQuery()) {

                        boolean found = false;
                        boolean bestPriceMarked = false;
                        int resultCount = 0;
                        int availableCount = 0;

                        StringBuilder results = new StringBuilder();

                        while (rs.next()) {

                            found = true;
                            resultCount++;

                            String name = rs.getString("name");
                            String strength = rs.getString("strength");
                            String pharmacy = rs.getString("pharmacy");
                            String address = rs.getString("address");
                            String pharmacyCity = rs.getString("city");
                            String phone = rs.getString("phone");

                            int quantity = rs.getInt("quantity");
                            java.math.BigDecimal price =
                                    rs.getBigDecimal("price");

                            boolean inStock = quantity > 0;
                            boolean bestPrice = inStock && !bestPriceMarked;

                            if (inStock) {
                                availableCount++;
                            }

                            if (bestPrice) {
                                bestPriceMarked = true;
                            }

                            results.append("<div class='card")
                                    .append(bestPrice ? " best" : "")
                                    .append("'>");

                            if (bestPrice) {
                                results.append("<span class='badge best-badge'>")
                                        .append("LOWEST PRICE — IN STOCK")
                                        .append("</span><br>");
                            } else if (inStock) {
                                results.append("<span class='badge stock-badge'>")
                                        .append("IN STOCK")
                                        .append("</span><br>");
                            } else {
                                results.append("<span class='badge out-badge'>")
                                        .append("OUT OF STOCK")
                                        .append("</span><br>");
                            }

                            results.append("<h2 class='medicine-title'>")
                                    .append(escapeHtml(name));

                            if (strength != null && !strength.trim().isEmpty()) {
                                results.append(" (")
                                        .append(escapeHtml(strength))
                                        .append(")");
                            }

                            results.append("</h2>");

                            results.append("<div class='details'>")
                                    .append("<b>Pharmacy:</b> ")
                                    .append(escapeHtml(pharmacy))
                                    .append("<br>");

                            results.append("<b>Address:</b> ")
                                    .append(escapeHtml(address))
                                    .append(", ")
                                    .append(escapeHtml(pharmacyCity))
                                    .append("<br>");

                            results.append("<b>Phone:</b> ")
                                    .append(escapeHtml(phone))
                                    .append("<br>");

                            results.append("<b>Available Quantity:</b> ")
                                    .append(quantity)
                                    .append("<br>");

                            results.append("<b>Price:</b> ");

                            if (price != null) {
                                results.append("<span class='price'>₹")
                                        .append(escapeHtml(price.toPlainString()))
                                        .append("</span>");
                            } else {
                                results.append("Not provided");
                            }

                            results.append("</div></div>");
                        }

                        if (!found) {
                            out.println("<div class='empty'>");
                            out.println("<h3>No matching medicine found.</h3>");
                            out.println("<p>Check the medicine name and city, "
                                    + "or try searching without a city.</p>");
                            out.println("</div>");
                        } else {
                            out.println("<div class='summary'>");
                            out.println("<b>Matching pharmacy records:</b> "
                                    + resultCount + "<br>");
                            out.println("<b>Records with stock available:</b> "
                                    + availableCount);
                            out.println("<p>Results with available stock and lower "
                                    + "prices appear first.</p>");
                            out.println("</div>");

                            out.println(results);
                        }
                    }

                } catch (Exception e) {
                    getServletContext().log("Medicine search failed", e);
                    response.setStatus(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("<div class='empty'>");
                    out.println("<h3>Search failed.</h3>");
                    out.println("<p>Please check the server logs.</p>");
                    out.println("</div>");
                }
            }

            out.println("<p><a class='back' href='"
                    + request.getContextPath()
                    + "/index.html'>Back to Home</a></p>");

            out.println("</div></body></html>");
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}