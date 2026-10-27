package com.medicinefinder.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.medicinefinder.database.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/admin-dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private int getCount(Connection con, String sql)
            throws SQLException {

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Admin login check
        if (session == null || session.getAttribute("adminId") == null) {
            response.sendRedirect(
                    request.getContextPath() + "/admin-login"
            );
            return;
        }

        int medicineCount = 0;
        int pharmacyCount = 0;
        int customerCount = 0;
        int requestCount = 0;
        String errorMessage = null;

        // Load current counts from MySQL
        try (Connection con = DBConnection.getConnection()) {

            medicineCount = getCount(
                    con, "SELECT COUNT(*) FROM medicines"
            );

            pharmacyCount = getCount(
                    con, "SELECT COUNT(*) FROM pharmacies"
            );

            customerCount = getCount(
                    con, "SELECT COUNT(*) FROM users"
            );

            requestCount = getCount(
                    con, "SELECT COUNT(*) FROM medicine_requests"
            );

        } catch (Exception e) {
            getServletContext().log(
                    "Error loading admin dashboard statistics", e
            );
            errorMessage = "Unable to load dashboard statistics. "
                    + "Please check the server logs.";
        }

        response.setContentType("text/html;charset=UTF-8");

        String contextPath = request.getContextPath();

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' "
                    + "content='width=device-width, initial-scale=1'>");
            out.println("<title>Admin Dashboard</title>");

            out.println("<style>");
            out.println("*{box-sizing:border-box}");
            out.println("body{margin:0;font-family:Arial,sans-serif;"
                    + "background:#f1f5f9;color:#1e293b;padding:20px}");
            out.println(".header{background:#0f766e;color:white;"
                    + "padding:24px;border-radius:12px;margin-bottom:20px}");
            out.println(".cards{display:grid;"
                    + "grid-template-columns:repeat(auto-fit,minmax(180px,1fr));"
                    + "gap:16px}");
            out.println(".card{background:white;padding:22px;"
                    + "border-radius:10px;box-shadow:0 2px 8px #0001}");
            out.println(".number{font-size:30px;font-weight:bold;"
                    + "margin-top:12px;color:#0f766e}");
            out.println(".section{background:white;padding:22px;"
                    + "border-radius:10px;margin-top:20px}");
            out.println(".links{display:grid;"
                    + "grid-template-columns:repeat(auto-fit,minmax(200px,1fr));"
                    + "gap:12px}");
            out.println(".link{display:block;padding:15px;"
                    + "background:#e0f2fe;color:#075985;"
                    + "text-decoration:none;border-radius:8px;font-weight:bold}");
            out.println(".link:hover{background:#bae6fd}");
            out.println(".analytics{background:#ccfbf1;color:#115e59}");
            out.println(".logout{background:#fee2e2;color:#991b1b;"
                    + "border:0;padding:12px 18px;border-radius:7px;"
                    + "font-weight:bold;cursor:pointer}");
            out.println(".error{background:#fee2e2;color:#991b1b;"
                    + "padding:12px;border-radius:8px}");
            out.println("</style></head><body>");

            out.println("<div class='header'>");
            out.println("<h1>Medicine Finder - Admin Dashboard</h1>");
            out.println("<p>Manage medicines, pharmacies, customers "
                    + "and medicine requests.</p>");
            out.println("</div>");

            if (errorMessage != null) {
                out.println("<p class='error'>"
                        + errorMessage + "</p>");
            }

            out.println("<div class='cards'>");

            out.println("<div class='card'>Total Medicines"
                    + "<div class='number'>" + medicineCount
                    + "</div></div>");

            out.println("<div class='card'>Total Pharmacies"
                    + "<div class='number'>" + pharmacyCount
                    + "</div></div>");

            out.println("<div class='card'>Registered Customers"
                    + "<div class='number'>" + customerCount
                    + "</div></div>");

            out.println("<div class='card'>Medicine Requests"
                    + "<div class='number'>" + requestCount
                    + "</div></div>");

            out.println("</div>");

            out.println("<div class='section'>");
            out.println("<h2>Management</h2>");
            out.println("<div class='links'>");

            out.println("<a class='link' href='" + contextPath
                    + "/admin-medicines'>Manage Medicines</a>");

            out.println("<a class='link' href='" + contextPath
                    + "/admin-pharmacies'>Manage Pharmacies</a>");

            out.println("<a class='link' href='" + contextPath
                    + "/admin-customers'>Manage Customers</a>");

            out.println("<a class='link' href='" + contextPath
                    + "/admin-requests'>Manage Medicine Requests</a>");

            out.println("<a class='link' href='" + contextPath
                    + "/index.html'>Open Main Website</a>");

            // Python analytics service running on this computer
            out.println("<a class='link analytics' "
                    + "href='http://127.0.0.1:5000/' "
                    + "target='_blank' rel='noopener noreferrer'>"
                    + "Python Stock Analytics &#8599;</a>");

            out.println("</div></div>");

            out.println("<div class='section'>");
            out.println("<h2>Admin Account</h2>");
            out.println("<form method='post' action='"
                    + contextPath + "/admin-logout'>");
            out.println("<button class='logout' type='submit'>"
                    + "Logout</button>");
            out.println("</form></div>");

            out.println("</body></html>");
        }
    }
}