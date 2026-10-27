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

@WebServlet("/customer-request-history")
public class CustomerRequestHistoryServlet extends HttpServlet {

    private String escapeHtml(String value) {
        if (value == null) {
            return "-";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String statusClass(String status) {
        if (status == null) {
            return "pending";
        }

        switch (status.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "available":
                return "available";
            case "unavailable":
                return "unavailable";
            default:
                return "pending";
        }
    }

    private String statusMessage(String status) {
        if (status == null) {
            return "Your request is awaiting an update.";
        }

        switch (status.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "available":
                return "Good news! Your requested medicine is marked available.";
            case "unavailable":
                return "The requested medicine is currently marked unavailable.";
            default:
                return "Your request is pending. Please check again later.";
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("customerId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/customer-login.html");
            return;
        }

        int customerId = ((Number)
                session.getAttribute("customerId")).intValue();

        response.setContentType("text/html;charset=UTF-8");

        String sql =
                "SELECT id, medicine_name, city, additional_details, "
                + "status, created_at "
                + "FROM medicine_requests "
                + "WHERE customer_id = ? "
                + "ORDER BY created_at DESC";

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html><html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>My Medicine Notifications</title>");

            out.println("<style>");
            out.println("*{box-sizing:border-box}");
            out.println("body{font-family:Arial,sans-serif;background:#f2f6fc;margin:0;padding:22px;color:#263445}");
            out.println(".container{max-width:1100px;margin:auto;background:white;padding:24px;border-radius:14px;box-shadow:0 4px 18px #dce3ed}");
            out.println("h2{color:#173b65;margin-top:0}");
            out.println(".intro{color:#64748b;line-height:1.5}");
            out.println(".links{display:flex;flex-wrap:wrap;gap:10px;margin:20px 0}");
            out.println(".link{display:inline-block;padding:10px 14px;background:#173b65;color:white;text-decoration:none;border-radius:7px}");
            out.println(".link:hover{background:#245889}");
            out.println(".cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:14px;margin:20px 0}");
            out.println(".notice{padding:16px;border-radius:10px;border:1px solid;margin-bottom:14px}");
            out.println(".notice h3{margin:0 0 8px;font-size:17px}");
            out.println(".notice p{margin:5px 0;line-height:1.5}");
            out.println(".available{background:#f0fdf4;border-color:#bbf7d0;color:#166534}");
            out.println(".unavailable{background:#fef2f2;border-color:#fecaca;color:#991b1b}");
            out.println(".pending{background:#fff7ed;border-color:#fed7aa;color:#9a3412}");
            out.println(".table-wrap{overflow-x:auto}");
            out.println("table{width:100%;border-collapse:collapse;min-width:750px;margin-top:14px}");
            out.println("th,td{border:1px solid #e2e8f0;padding:11px;text-align:left;vertical-align:top}");
            out.println("th{background:#173b65;color:white}");
            out.println("tr:nth-child(even){background:#f8fafc}");
            out.println(".badge{display:inline-block;padding:5px 9px;border-radius:20px;font-size:12px;font-weight:bold}");
            out.println(".empty{padding:20px;text-align:center;background:#f8fafc;border-radius:8px;color:#64748b}");
            out.println("@media(max-width:600px){.container{padding:15px}}");
            out.println("</style></head><body><div class='container'>");

            out.println("<h2>My Medicine Requests & Notifications</h2>");
            out.println("<p class='intro'>View your medicine requests and the latest status reported by the system. Refresh this page to check for updates.</p>");

            out.println("<div class='links'>");
            out.println("<a class='link' href='"
                    + request.getContextPath()
                    + "/customer-dashboard'>Customer Dashboard</a>");
            out.println("<a class='link' href='"
                    + request.getContextPath()
                    + "/medicine-request.html'>Request Another Medicine</a>");
            out.println("</div>");

            int total = 0;
            int availableCount = 0;
            int unavailableCount = 0;
            int pendingCount = 0;

            StringBuilder cards = new StringBuilder();
            StringBuilder rows = new StringBuilder();

            boolean queryFailed = false;

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, customerId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        total++;

                        String medicine = rs.getString("medicine_name");
                        String city = rs.getString("city");
                        String details = rs.getString("additional_details");
                        String status = rs.getString("status");
                        String createdAt = String.valueOf(
                                rs.getTimestamp("created_at"));

                        String css = statusClass(status);
                        String message = statusMessage(status);

                        if ("available".equals(css)) {
                            availableCount++;
                        } else if ("unavailable".equals(css)) {
                            unavailableCount++;
                        } else {
                            pendingCount++;
                        }

                        // Notification card
                        cards.append("<div class='notice ")
                                .append(css).append("'>")
                                .append("<h3>")
                                .append(escapeHtml(medicine))
                                .append(" — ")
                                .append(escapeHtml(status))
                                .append("</h3>")
                                .append("<p>")
                                .append(escapeHtml(message))
                                .append("</p>")
                                .append("<p><strong>City:</strong> ")
                                .append(escapeHtml(city))
                                .append("</p>")
                                .append("<p><strong>Request ID:</strong> ")
                                .append(rs.getInt("id"))
                                .append("</p></div>");

                        // History table row
                        rows.append("<tr><td>")
                                .append(rs.getInt("id"))
                                .append("</td><td>")
                                .append(escapeHtml(medicine))
                                .append("</td><td>")
                                .append(escapeHtml(city))
                                .append("</td><td>")
                                .append(escapeHtml(details))
                                .append("</td><td><span class='badge ")
                                .append(css).append("'>")
                                .append(escapeHtml(status))
                                .append("</span></td><td>")
                                .append(escapeHtml(createdAt))
                                .append("</td></tr>");
                    }
                }

            } catch (Exception e) {
                log("Error loading customer request history", e);
                queryFailed = true;
            }

            if (queryFailed) {
                out.println("<div class='notice unavailable'>");
                out.println("<h3>Unable to load requests</h3>");
                out.println("<p>Please try again later or check the server log.</p>");
                out.println("</div>");
            } else {
                out.println("<div class='cards'>");
                out.println("<div class='notice pending'><h3>Total Requests</h3><p>"
                        + total + "</p></div>");
                out.println("<div class='notice available'><h3>Available</h3><p>"
                        + availableCount + "</p></div>");
                out.println("<div class='notice unavailable'><h3>Unavailable</h3><p>"
                        + unavailableCount + "</p></div>");
                out.println("<div class='notice pending'><h3>Pending</h3><p>"
                        + pendingCount + "</p></div>");
                out.println("</div>");

                out.println("<h3>Latest Status Notifications</h3>");

                if (total == 0) {
                    out.println("<div class='empty'>You have not submitted any medicine requests yet.</div>");
                } else {
                    out.println(cards);
                }

                out.println("<h3>Request History</h3>");
                out.println("<div class='table-wrap'><table>");
                out.println("<thead><tr><th>Request ID</th><th>Medicine</th>"
                        + "<th>City</th><th>Details</th><th>Status</th>"
                        + "<th>Submitted At</th></tr></thead><tbody>");
                if (total == 0) {
                    out.println("<tr><td colspan='6'>No requests found.</td></tr>");
                } else {
                    out.println(rows);
                }
                out.println("</tbody></table></div>");
            }

            out.println("</div></body></html>");
        }
    }
}