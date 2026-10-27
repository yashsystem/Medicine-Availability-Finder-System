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

@WebServlet("/admin-customers")
public class AdminCustomersServlet extends HttpServlet {

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
            out.println("<title>Manage Customers</title>");
            out.println("<style>");
            out.println("body{font-family:Arial,sans-serif;background:#f1f5f9;padding:20px}");
            out.println(".box{max-width:1000px;margin:20px auto;background:white;padding:24px;border-radius:10px}");
            out.println(".wrap{overflow-x:auto}");
            out.println("table{width:100%;border-collapse:collapse;margin-top:20px}");
            out.println("th,td{padding:10px;border:1px solid #ddd;text-align:left}");
            out.println("th{background:#173b65;color:white}");
            out.println(".btn{display:inline-block;padding:10px 14px;background:#173b65;color:white;text-decoration:none;border-radius:5px}");
            out.println("</style></head><body>");

            out.println("<div class='box'>");
            out.println("<h1>Manage Customers</h1>");
            out.println("<p>Registered customer accounts</p>");

            out.println("<div class='wrap'><table>");
            out.println("<tr><th>ID</th><th>Full Name</th><th>Email</th><th>Registered On</th></tr>");

            String sql = "SELECT id, full_name, email, created_at "
                    + "FROM users ORDER BY created_at DESC";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("id") + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("full_name")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("email")) + "</td>");
                    out.println("<td>" + escapeHtml(
                            String.valueOf(rs.getTimestamp("created_at"))) + "</td>");
                    out.println("</tr>");
                }

            } catch (Exception e) {
                log("Unable to load customer list", e);
                out.println("</table></div>");
                out.println("<p style='color:red'>Could not load customers. Check the server log.</p>");
                out.println("<p><a class='btn' href='" + ctx
                        + "/admin-dashboard'>Back to Dashboard</a></p>");
                out.println("</div></body></html>");
                return;
            }

            out.println("</table></div>");
            out.println("<p><a class='btn' href='" + ctx
                    + "/admin-dashboard'>Back to Dashboard</a></p>");
            out.println("</div></body></html>");
        }
    }
}