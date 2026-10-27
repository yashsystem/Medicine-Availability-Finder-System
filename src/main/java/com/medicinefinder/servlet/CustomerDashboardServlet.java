package com.medicinefinder.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/customer-dashboard")
public class CustomerDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null
                || session.getAttribute("customerId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/customer-login.html");
            return;
        }

        String customerName =
                (String) session.getAttribute("customerName");

        String safeName = escapeHtml(customerName);
        String contextPath = request.getContextPath();

        try (PrintWriter out = response.getWriter()) {

            out.println("<!DOCTYPE html>");
            out.println("<html lang='en'>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' "
                    + "content='width=device-width, initial-scale=1.0'>");
            out.println("<title>Customer Dashboard</title>");

            out.println("<style>");
            out.println("body{font-family:Arial,sans-serif;"
                    + "background:#f1f5f9;margin:0;padding:20px;}");
            out.println(".container{max-width:700px;margin:40px auto;"
                    + "background:white;padding:30px;border-radius:10px;"
                    + "box-shadow:0 4px 15px rgba(0,0,0,.1);}");
            out.println("h1{color:#087f5b;}");
            out.println(".link{display:inline-block;background:#087f5b;"
                    + "color:white;padding:12px 18px;margin:8px 5px 8px 0;"
                    + "text-decoration:none;border-radius:5px;}");
            out.println(".link:hover{background:#066849;}");
            out.println(".logout{background:#b91c1c;border:none;"
                    + "color:white;padding:12px 18px;border-radius:5px;"
                    + "cursor:pointer;font-size:15px;}");
            out.println("</style>");

            out.println("</head><body>");
            out.println("<div class='container'>");

            out.println("<h1>Welcome, " + safeName + "!</h1>");
            out.println("<p>You are successfully logged in.</p>");
            out.println("<p>Welcome to the Medicine Availability Finder "
                    + "System. Find medicines available at nearby "
                    + "pharmacies.</p>");

            out.println("<a class='link' href='" + contextPath
                    + "/index.html'>Search Medicines</a>");

            out.println("<a class='link' href='" + contextPath
                    + "/medicine-request.html'>Request a Medicine</a>");

            out.println("<a class='link' href='" + contextPath
                    + "/customer-request-history'>My Request History</a>");

            out.println("<form action='" + contextPath
                    + "/customer-logout' method='post'>");

            out.println("<button class='logout' type='submit'>"
                    + "Logout</button>");

            out.println("</form>");
            out.println("</div></body></html>");
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "Customer";
        }

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}