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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/owner-requests")
public class OwnerRequestsServlet extends HttpServlet {

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

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("ownerId") == null
                || session.getAttribute("pharmacyId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/owner-login.html");
            return;
        }

        int pharmacyId = ((Number)
                session.getAttribute("pharmacyId")).intValue();

        response.setContentType("text/html;charset=UTF-8");

        String message = request.getParameter("message");

        String sql =
                "SELECT r.id, r.medicine_name, r.city, " +
                "r.additional_details, r.status, r.created_at " +
                "FROM medicine_requests r " +
                "JOIN pharmacies p ON " +
                "LOWER(TRIM(r.city)) = LOWER(TRIM(p.city)) " +
                "WHERE p.id = ? " +
                "ORDER BY r.created_at DESC";

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html><html><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' " +
                    "content='width=device-width, initial-scale=1'>");
            out.println("<title>Customer Medicine Requests</title>");

            out.println("<style>");
            out.println("body{font-family:Arial,sans-serif;" +
                    "background:#f2f6fc;margin:0;padding:20px;}");
            out.println(".container{max-width:1200px;margin:auto;" +
                    "background:white;padding:22px;border-radius:12px;}");
            out.println("h2{color:#173b65;}");
            out.println(".table-wrap{overflow-x:auto;}");
            out.println("table{width:100%;border-collapse:collapse;" +
                    "min-width:850px;}");
            out.println("th,td{border:1px solid #ddd;padding:10px;" +
                    "text-align:left;}");
            out.println("th{background:#173b65;color:white;}");
            out.println("tr:nth-child(even){background:#f5f7fa;}");
            out.println(".link,.btn{display:inline-block;padding:9px 12px;" +
                    "background:#173b65;color:white;text-decoration:none;" +
                    "border:0;border-radius:5px;cursor:pointer;}");
            out.println("select{padding:8px;margin:3px;}");
            out.println(".message{padding:12px;background:#e8f5e9;" +
                    "border-radius:6px;margin:12px 0;}");
            out.println("</style></head><body>");

            out.println("<div class='container'>");
            out.println("<h2>Customer Medicine Requests</h2>");
            out.println("<p>Review requests from customers in your city " +
                    "and update their status.</p>");

            out.println("<a class='link' href='" +
                    request.getContextPath() +
                    "/owner-dashboard'>Back to Dashboard</a>");

            if ("updated".equals(message)) {
                out.println("<div class='message'>Request status updated successfully.</div>");
            } else if ("invalid".equals(message)) {
                out.println("<div class='message'>Invalid request or status.</div>");
            } else if ("error".equals(message)) {
                out.println("<div class='message'>Could not update status. Please check server logs.</div>");
            }

            out.println("<div class='table-wrap'><table>");
            out.println("<tr>");
            out.println("<th>Request ID</th>");
            out.println("<th>Medicine</th>");
            out.println("<th>City</th>");
            out.println("<th>Details</th>");
            out.println("<th>Current Status</th>");
            out.println("<th>Update Status</th>");
            out.println("<th>Requested At</th>");
            out.println("</tr>");

            boolean found = false;

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, pharmacyId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        found = true;

                        int requestId = rs.getInt("id");
                        String status = rs.getString("status");

                        out.println("<tr>");
                        out.println("<td>" + requestId + "</td>");
                        out.println("<td>" +
                                escapeHtml(rs.getString("medicine_name")) +
                                "</td>");
                        out.println("<td>" +
                                escapeHtml(rs.getString("city")) +
                                "</td>");
                        out.println("<td>" +
                                escapeHtml(rs.getString("additional_details")) +
                                "</td>");
                        out.println("<td>" + escapeHtml(status) + "</td>");

                        out.println("<td>");
                        out.println("<form action='" +
                                request.getContextPath() +
                                "/update-request-status' method='post'>");

                        out.println("<input type='hidden' name='requestId' value='" +
                                requestId + "'>");

                        out.println("<select name='status' required>");

                        String[] statuses = {
                            "Pending", "Available", "Unavailable"
                        };

                        for (String option : statuses) {
                            out.println("<option value='" + option + "'" +
                                    (option.equals(status) ? " selected" : "") +
                                    ">" + option + "</option>");
                        }

                        out.println("</select>");
                        out.println("<button class='btn' type='submit'>Update</button>");
                        out.println("</form>");
                        out.println("</td>");

                        out.println("<td>" +
                                escapeHtml(String.valueOf(
                                        rs.getTimestamp("created_at"))) +
                                "</td>");
                        out.println("</tr>");
                    }
                }

            } catch (Exception e) {
                log("Error loading customer medicine requests", e);
                out.println("<tr><td colspan='7'>");
                out.println("Could not load requests. Check server logs.");
                out.println("</td></tr>");
            }

            if (!found) {
                out.println("<tr><td colspan='7'>");
                out.println("No medicine requests found for your city.");
                out.println("</td></tr>");
            }

            out.println("</table></div></div></body></html>");
        }
    }
}