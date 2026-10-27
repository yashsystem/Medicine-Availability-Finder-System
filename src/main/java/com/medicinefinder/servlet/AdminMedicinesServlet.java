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
import java.sql.SQLException;

@WebServlet("/admin-medicines")
public class AdminMedicinesServlet extends HttpServlet {

    private String escapeHtml(String value) {
        if (value == null) return "";

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean isAdminLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("adminId") != null;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        if (!isAdminLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/admin-login.html");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            String ctx = request.getContextPath();

            out.println("<!DOCTYPE html><html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Manage Medicines</title>");
            out.println("<style>");
            out.println("body{font-family:Arial;background:#f1f5f9;padding:20px}");
            out.println(".box{max-width:1000px;margin:20px auto;background:white;padding:24px;border-radius:10px}");
            out.println("input{padding:10px;margin:5px;width:90%;max-width:350px;box-sizing:border-box}");
            out.println("button,.btn{padding:10px 14px;background:#173b65;color:white;border:0;border-radius:5px;text-decoration:none;cursor:pointer}");
            out.println(".delete{background:#b42318}");
            out.println("table{width:100%;border-collapse:collapse;margin-top:20px}");
            out.println("th,td{padding:10px;border:1px solid #ddd;text-align:left}");
            out.println("th{background:#173b65;color:white}");
            out.println(".tablewrap{overflow-x:auto}");
            out.println("</style></head><body><div class='box'>");

            out.println("<h1>Manage Medicines</h1>");
            out.println("<h2>Add Medicine</h2>");
            out.println("<form method='post' action='" + ctx + "/admin-medicines'>");
            out.println("<input type='hidden' name='action' value='add'>");
            out.println("<input name='name' placeholder='Medicine name' required maxlength='150'><br>");
            out.println("<input name='strength' placeholder='Strength (e.g. 500 mg)' maxlength='50'><br>");
            out.println("<input name='manufacturer' placeholder='Manufacturer' maxlength='150'><br>");
            out.println("<button type='submit'>Add Medicine</button>");
            out.println("</form>");

            String message = request.getParameter("message");

            if ("added".equals(message)) {
                out.println("<p style='color:green'>Medicine added successfully.</p>");
            } else if ("deleted".equals(message)) {
                out.println("<p style='color:green'>Medicine deleted successfully.</p>");
            } else if ("linked".equals(message)) {
                out.println("<p style='color:#b45309'>Cannot delete this medicine because it is linked to pharmacy stock. Update or remove the linked stock first.</p>");
            } else if ("invalid".equals(message)) {
                out.println("<p style='color:red'>Invalid request. Please try again.</p>");
            } else if ("error".equals(message)) {
                out.println("<p style='color:red'>Operation failed. Check the server log.</p>");
            }

            out.println("<h2>Medicine List</h2>");
            out.println("<div class='tablewrap'><table>");
            out.println("<tr><th>ID</th><th>Name</th><th>Strength</th><th>Manufacturer</th><th>Action</th></tr>");

            String sql = "SELECT id, name, strength, manufacturer FROM medicines ORDER BY name";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    long id = rs.getLong("id");

                    out.println("<tr>");
                    out.println("<td>" + id + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("name")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("strength")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("manufacturer")) + "</td>");
                    out.println("<td><form method='post' action='" + ctx
                            + "/admin-medicines' onsubmit=\"return confirm('Delete this medicine?');\">");
                    out.println("<input type='hidden' name='action' value='delete'>");
                    out.println("<input type='hidden' name='medicineId' value='" + id + "'>");
                    out.println("<button class='delete' type='submit'>Delete</button>");
                    out.println("</form></td>");
                    out.println("</tr>");
                }

            } catch (Exception e) {
                log("Unable to load medicines", e);
                out.println("</table></div><p style='color:red'>Unable to load medicines. Check server logs.</p>");
                out.println("<p><a class='btn' href='" + ctx + "/admin-dashboard'>Back to Dashboard</a></p>");
                out.println("</div></body></html>");
                return;
            }

            out.println("</table></div>");
            out.println("<p><a class='btn' href='" + ctx + "/admin-dashboard'>Back to Dashboard</a></p>");
            out.println("</div></body></html>");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        if (!isAdminLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/admin-login.html");
            return;
        }

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        String ctx = request.getContextPath();

        if ("add".equals(action)) {
            String name = request.getParameter("name");
            String strength = request.getParameter("strength");
            String manufacturer = request.getParameter("manufacturer");

            if (name == null || name.trim().isEmpty()) {
                response.sendRedirect(ctx + "/admin-medicines?message=invalid");
                return;
            }

            String sql = "INSERT INTO medicines (name, strength, manufacturer) VALUES (?, ?, ?)";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, name.trim());
                ps.setString(2, strength == null || strength.trim().isEmpty()
                        ? null : strength.trim());
                ps.setString(3, manufacturer == null || manufacturer.trim().isEmpty()
                        ? null : manufacturer.trim());

                ps.executeUpdate();
                response.sendRedirect(ctx + "/admin-medicines?message=added");

            } catch (Exception e) {
                log("Unable to add medicine", e);
                response.sendRedirect(ctx + "/admin-medicines?message=error");
            }

            return;
        }

        if ("delete".equals(action)) {
            String idText = request.getParameter("medicineId");
            long medicineId;

            try {
                medicineId = Long.parseLong(idText);
                if (medicineId <= 0) throw new NumberFormatException();
            } catch (NumberFormatException | NullPointerException e) {
                response.sendRedirect(ctx + "/admin-medicines?message=invalid");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {
                // Check first so linked medicine records are preserved.
                String checkSql = "SELECT COUNT(*) FROM pharmacy_stock WHERE medicine_id = ?";

                try (PreparedStatement check = con.prepareStatement(checkSql)) {
                    check.setLong(1, medicineId);

                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            response.sendRedirect(ctx + "/admin-medicines?message=linked");
                            return;
                        }
                    }
                }

                String deleteSql = "DELETE FROM medicines WHERE id = ?";

                try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setLong(1, medicineId);

                    if (ps.executeUpdate() == 1) {
                        response.sendRedirect(ctx + "/admin-medicines?message=deleted");
                    } else {
                        response.sendRedirect(ctx + "/admin-medicines?message=invalid");
                    }
                }

            } catch (SQLException e) {
                log("Unable to delete medicine", e);
                response.sendRedirect(ctx + "/admin-medicines?message=error");
            } catch (Exception e) {
                log("Unexpected error while deleting medicine", e);
                response.sendRedirect(ctx + "/admin-medicines?message=error");
            }

            return;
        }

        response.sendRedirect(ctx + "/admin-medicines?message=invalid");
    }
}