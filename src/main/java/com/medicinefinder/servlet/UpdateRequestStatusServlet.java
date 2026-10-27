package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/update-request-status")
public class UpdateRequestStatusServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String contextPath = request.getContextPath();

        HttpSession session = request.getSession(false);

        // Check owner login
        if (session == null
                || session.getAttribute("ownerId") == null
                || session.getAttribute("pharmacyId") == null) {

            response.sendRedirect(contextPath + "/owner-login.html");
            return;
        }

        int pharmacyId = ((Number)
                session.getAttribute("pharmacyId")).intValue();

        // Read submitted values
        String requestIdText = request.getParameter("requestId");
        String newStatus = request.getParameter("status");

        // Allow only these status values
        if (requestIdText == null
                || !requestIdText.matches("[0-9]+")
                || newStatus == null
                || !(newStatus.equals("Pending")
                || newStatus.equals("Available")
                || newStatus.equals("Unavailable"))) {

            response.sendRedirect(
                    contextPath + "/owner-requests?message=invalid");
            return;
        }

        int requestId;

        try {
            requestId = Integer.parseInt(requestIdText);
        } catch (NumberFormatException e) {
            response.sendRedirect(
                    contextPath + "/owner-requests?message=invalid");
            return;
        }

        String sql =
                "UPDATE medicine_requests r " +
                "JOIN pharmacies p ON " +
                "LOWER(TRIM(r.city)) = LOWER(TRIM(p.city)) " +
                "SET r.status = ? " +
                "WHERE r.id = ? AND p.id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, requestId);
            ps.setInt(3, pharmacyId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                response.sendRedirect(
                        contextPath + "/owner-requests?message=updated");
            } else {
                response.sendRedirect(
                        contextPath + "/owner-requests?message=invalid");
            }

        } catch (Exception e) {
            log("Error updating medicine request status", e);

            response.sendRedirect(
                    contextPath + "/owner-requests?message=error");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.sendRedirect(
                request.getContextPath() + "/owner-requests");
    }
}