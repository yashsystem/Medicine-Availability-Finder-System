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

@WebServlet("/admin-requests")
public class AdminRequestsServlet extends HttpServlet {

    private String escapeHtml(String value) {
        if (value == null) return "";

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("adminId") == null) {
            response.sendRedirect(
                    request.getContextPath() + "/admin-login.html");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            String ctx = request.getContextPath();

            out.println("<!DOCTYPE html><html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Manage Medicine Requests</title>");
            out.println("<style>");
            out.println("body{font-family:Arial,sans-serif;background:#f1f5f9;padding:20px}");
            out.println(".box{max-width:1200px;margin:20px auto;background:white;padding:24px;border-radius:10px}");
            out.println(".wrap{overflow-x:auto}");
            out.println("table{width:100%;border-collapse:collapse;margin-top:20px}");
            out.println("th,td{padding:10px;border:1px solid #ddd;text-align:left}");
            out.println("th{background:#173b65;color:white}");
            out.println(".btn{display:inline-block;padding:10px 14px;background:#173b65;color:white;text-decoration:none;border-radius:5px}");
            out.println("</style></head><body>");

            out.println("<div class='box'>");
            out.println("<h1>Manage Medicine Requests</h1>");
            out.println("<p>All customer medicine requests</p>");

            String message = request.getParameter("message");
            if ("updated".equals(message)) {
                out.println("<p style='color:green'>Request status updated.</p>");
            } else if ("error".equals(message)) {
                out.println("<p style='color:red'>Could not update status. Check the server log.</p>");
            }

            out.println("<div class='wrap'><table>");
            out.println("<tr><th>ID</th><th>Customer</th><th>Medicine</th><th>City</th><th>Details</th><th>Status</th><th>Requested On</th><th>Update</th></tr>");

            String sql = "SELECT r.id, u.full_name, r.medicine_name, "
                    + "r.city, r.additional_details, r.status, r.created_at "
                    + "FROM medicine_requests r "
                    + "JOIN users u ON r.customer_id = u.id "
                    + "ORDER BY r.created_at DESC";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String status = rs.getString("status");

                    out.println("<tr>");
                    out.println("<td>" + id + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("full_name")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("medicine_name")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("city")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("additional_details")) + "</td>");
                    out.println("<td>" + escapeHtml(status) + "</td>");
                    out.println("<td>" + escapeHtml(String.valueOf(rs.getTimestamp("created_at"))) + "</td>");

                    out.println("<td><form method='post' action='" + ctx + "/admin-request-status'>");
                    out.println("<input type='hidden' name='requestId' value='" + id + "'>");
                    out.println("<select name='status' required>");
                    out.println("<option value='Pending'" + ("Pending".equals(status) ? " selected" : "") + ">Pending</option>");
                    out.println("<option value='Available'" + ("Available".equals(status) ? " selected" : "") + ">Available</option>");
                    out.println("<option value='Unavailable'" + ("Unavailable".equals(status) ? " selected" : "") + ">Unavailable</option>");
                    out.println("</select> ");
                    out.println("<button type='submit'>Save</button>");
                    out.println("</form></td>");
                    out.println("</tr>");
                }

            } catch (Exception e) {
                log("Unable to load medicine requests", e);
                out.println("</table></div>");
                out.println("<p style='color:red'>Could not load requests. Check the server log.</p>");
                out.println("<p><a class='btn' href='" + ctx + "/admin-dashboard'>Back to Dashboard</a></p>");
                out.println("</div></body></html>");
                return;
            }

            out.println("</table></div>");
            out.println("<p><a class='btn' href='" + ctx + "/admin-dashboard'>Back to Dashboard</a></p>");
            out.println("</div></body></html>");
        }
    }
}