package com.medicinefinder.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/owner-dashboard")
public class OwnerDashboardServlet extends HttpServlet {

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

        response.setContentType("text/html;charset=UTF-8");

        String ownerName = escapeHtml(
                (String) session.getAttribute("ownerName"));

        String contextPath = request.getContextPath();

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html><html><head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' "
                    + "content='width=device-width, initial-scale=1'>");
            out.println("<title>Owner Dashboard</title>");

            out.println("<style>");
            out.println("body{font-family:Arial,sans-serif;"
                    + "background:#f2f6fc;padding:25px;margin:0;}");
            out.println(".box{max-width:700px;margin:30px auto;"
                    + "background:white;padding:28px;border-radius:12px;"
                    + "box-shadow:0 3px 12px #0001;}");
            out.println("h2{color:#173b65;}");
            out.println(".link{display:inline-block;margin:8px 8px 8px 0;"
                    + "padding:12px 16px;background:#173b65;color:white;"
                    + "text-decoration:none;border-radius:6px;}");
            out.println(".link:hover{background:#24578e;}");
            out.println(".logout{margin-top:18px;padding:10px 18px;"
                    + "background:#b42318;color:white;border:0;"
                    + "border-radius:5px;cursor:pointer;}");
            out.println("</style></head><body>");

            out.println("<div class='box'>");
            out.println("<h2>Welcome, " + ownerName + "!</h2>");
            out.println("<p>You are successfully logged in.</p>");
            out.println("<p>Manage your pharmacy:</p>");

            out.println("<a class='link' href='" + contextPath
                    + "/add-stock.html'>Add Medicine</a>");

            out.println("<a class='link' href='" + contextPath
                    + "/update-stock.html'>Update Stock</a>");

            out.println("<a class='link' href='" + contextPath
                    + "/stock-dashboard'>View Stock Dashboard</a>");

            out.println("<a class='link' href='" + contextPath
                    + "/owner-requests'>View Customer Requests</a>");

            out.println("<form action='" + contextPath
                    + "/owner-logout' method='post'>");

            out.println("<button class='logout' type='submit'>Logout</button>");
            out.println("</form>");

            out.println("</div></body></html>");
        }
    }
}