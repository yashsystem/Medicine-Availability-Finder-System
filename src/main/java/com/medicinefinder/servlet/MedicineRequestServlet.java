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
import java.sql.SQLException;

@WebServlet("/medicine-request")
public class MedicineRequestServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        HttpSession session = request.getSession(false);

        // Only logged-in customers can submit requests.
        if (session == null
                || session.getAttribute("customerId") == null) {

            response.sendRedirect(
                    request.getContextPath() + "/customer-login.html");
            return;
        }

        Integer customerId =
                (Integer) session.getAttribute("customerId");

        String medicineName = request.getParameter("medicine_name");
        String city = request.getParameter("city");
        String details = request.getParameter("additional_details");

        try (PrintWriter out = response.getWriter()) {

            if (medicineName == null || city == null
                    || medicineName.trim().isEmpty()
                    || city.trim().isEmpty()) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST);

                out.println("<h3>Medicine name and city are required.</h3>");
                out.println("<a href='medicine-request.html'>Go Back</a>");
                return;
            }

            medicineName = medicineName.trim();
            city = city.trim();
            details = details == null ? "" : details.trim();

            if (medicineName.length() > 150
                    || city.length() > 100
                    || details.length() > 500) {

                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST);

                out.println("<h3>Input is too long.</h3>");
                out.println("<a href='medicine-request.html'>Go Back</a>");
                return;
            }

            String sql = "INSERT INTO medicine_requests "
                    + "(customer_id, medicine_name, city, "
                    + "additional_details) VALUES (?, ?, ?, ?)";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, customerId);
                ps.setString(2, medicineName);
                ps.setString(3, city);

                if (details.isEmpty()) {
                    ps.setNull(4, java.sql.Types.VARCHAR);
                } else {
                    ps.setString(4, details);
                }

                ps.executeUpdate();

                out.println("<!DOCTYPE html>");
                out.println("<html lang='en'><head>");
                out.println("<meta charset='UTF-8'>");
                out.println("<meta name='viewport' "
                        + "content='width=device-width, initial-scale=1.0'>");
                out.println("<title>Request Submitted</title>");
                out.println("</head>");
                out.println("<body style='font-family:Arial;"
                        + "text-align:center;margin:60px 15px;'>");

                out.println("<h2 style='color:green;'>"
                        + "Medicine Request Submitted Successfully!</h2>");
                out.println("<p>Your request has been saved.</p>");
                out.println("<p>Status: <strong>Pending</strong></p>");
                out.println("<a href='medicine-request.html'>"
                        + "Submit Another Request</a><br><br>");
                out.println("<a href='customer-dashboard'>"
                        + "Customer Dashboard</a>");

                out.println("</body></html>");

            } catch (SQLException e) {
                log("Could not save medicine request.", e);

                response.setStatus(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                out.println("<h3>Request could not be saved.</h3>");
                out.println("<p>Please check the server logs.</p>");
                out.println("<a href='medicine-request.html'>Go Back</a>");
            }
        }
    }
}