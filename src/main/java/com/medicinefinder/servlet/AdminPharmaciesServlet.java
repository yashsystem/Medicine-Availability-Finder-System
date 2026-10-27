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

@WebServlet("/admin-pharmacies")
public class AdminPharmaciesServlet extends HttpServlet {

    private String escapeHtml(String value) {
        if (value == null) return "";

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean isAdmin(HttpServletRequest request,
                            HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("adminId") == null) {
            response.sendRedirect(
                    request.getContextPath() + "/admin-login.html");
            return false;
        }

        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        if (!isAdmin(request, response)) return;

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            String ctx = request.getContextPath();

            out.println("<!DOCTYPE html><html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<title>Manage Pharmacies</title>");

            out.println("<style>");
            out.println("body{font-family:Arial;background:#f1f5f9;padding:20px;color:#243447}");
            out.println(".box{max-width:1100px;margin:20px auto;background:white;padding:24px;border-radius:10px}");
            out.println("input{width:100%;padding:11px;margin:7px 0 14px;box-sizing:border-box;border:1px solid #ccd6dd;border-radius:5px}");
            out.println("label{font-weight:bold}");
            out.println("table{width:100%;border-collapse:collapse;margin-top:20px}");
            out.println("th,td{padding:10px;border:1px solid #ddd;text-align:left}");
            out.println("th{background:#173b65;color:white}");
            out.println(".wrap{overflow-x:auto}");
            out.println(".btn{display:inline-block;padding:11px 15px;background:#087f8c;color:white;text-decoration:none;border:0;border-radius:5px;cursor:pointer}");
            out.println(".formbox{background:#f8fafc;padding:18px;border-radius:8px;margin:20px 0}");
            out.println("</style></head><body>");

            out.println("<div class='box'>");
            out.println("<h1>Manage Pharmacies</h1>");
            out.println("<p>Add a new pharmacy or view registered pharmacies.</p>");

            String message = request.getParameter("message");

            if ("added".equals(message)) {
                out.println("<p style='color:green'>Pharmacy added successfully.</p>");
            } else if ("invalid".equals(message)) {
                out.println("<p style='color:red'>Please fill in all required fields.</p>");
            } else if ("error".equals(message)) {
                out.println("<p style='color:red'>Could not add pharmacy. Check server logs.</p>");
            }

            // Add Pharmacy Form
            out.println("<div class='formbox'>");
            out.println("<h2>Add New Pharmacy</h2>");
            out.println("<form method='post' action='" + ctx + "/admin-pharmacies'>");

            out.println("<label>Pharmacy Name *</label>");
            out.println("<input name='name' maxlength='150' required>");

            out.println("<label>Address</label>");
            out.println("<input name='address' maxlength='255'>");

            out.println("<label>City *</label>");
            out.println("<input name='city' maxlength='100' required>");

            out.println("<label>Phone</label>");
            out.println("<input name='phone' maxlength='20'>");

            out.println("<button class='btn' type='submit'>Add Pharmacy</button>");
            out.println("</form></div>");

            // Pharmacy List
            out.println("<h2>Registered Pharmacies</h2>");
            out.println("<div class='wrap'><table>");
            out.println("<tr><th>ID</th><th>Pharmacy Name</th><th>Address</th><th>City</th><th>Phone</th></tr>");

            String sql = "SELECT id, name, address, city, phone FROM pharmacies ORDER BY name";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    out.println("<tr>");
                    out.println("<td>" + rs.getInt("id") + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("name")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("address")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("city")) + "</td>");
                    out.println("<td>" + escapeHtml(rs.getString("phone")) + "</td>");
                    out.println("</tr>");
                }

            } catch (Exception e) {
                log("Unable to load pharmacies", e);
                out.println("</table></div>");
                out.println("<p style='color:red'>Could not load pharmacies. Check the server log.</p>");
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

        if (!isAdmin(request, response)) return;

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String address = request.getParameter("address");
        String city = request.getParameter("city");
        String phone = request.getParameter("phone");

        name = name == null ? "" : name.trim();
        address = address == null ? "" : address.trim();
        city = city == null ? "" : city.trim();
        phone = phone == null ? "" : phone.trim();

        String ctx = request.getContextPath();

        if (name.isEmpty() || city.isEmpty()
                || name.length() > 150
                || address.length() > 255
                || city.length() > 100
                || phone.length() > 20) {

            response.sendRedirect(ctx + "/admin-pharmacies?message=invalid");
            return;
        }

        String sql = "INSERT INTO pharmacies (name, address, city, phone) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, address.isEmpty() ? null : address);
            ps.setString(3, city);
            ps.setString(4, phone.isEmpty() ? null : phone);

            ps.executeUpdate();

            response.sendRedirect(ctx + "/admin-pharmacies?message=added");

        } catch (SQLException e) {
            log("Unable to add pharmacy", e);
            response.sendRedirect(ctx + "/admin-pharmacies?message=error");
        }
    }
}