package com.medicinefinder.servlet;

import com.medicinefinder.database.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/admin-request-status")
public class AdminRequestStatusServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        String ctx = request.getContextPath();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("adminId") == null) {
            response.sendRedirect(ctx + "/admin-login.html");
            return;
        }

        String idText = request.getParameter("requestId");
        String status = request.getParameter("status");

        int requestId;

        try {
            requestId = Integer.parseInt(idText);
            if (requestId <= 0) {
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            response.sendRedirect(ctx + "/admin-requests?message=error");
            return;
        }

        if (!"Pending".equals(status)
                && !"Available".equals(status)
                && !"Unavailable".equals(status)) {
            response.sendRedirect(ctx + "/admin-requests?message=error");
            return;
        }

        String sql = "UPDATE medicine_requests SET status = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, requestId);

            if (ps.executeUpdate() == 1) {
                response.sendRedirect(ctx + "/admin-requests?message=updated");
            } else {
                response.sendRedirect(ctx + "/admin-requests?message=error");
            }

        } catch (Exception e) {
            log("Unable to update medicine request status", e);
            response.sendRedirect(ctx + "/admin-requests?message=error");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/admin-requests");
    }
}