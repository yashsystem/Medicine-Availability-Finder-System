package com.medicinefinder.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = {
    "/add-stock",
    "/add-stock.html",
    "/update-stock",
    "/update-stock.html",
    "/stock-dashboard"
})
public class OwnerAuthFilter implements Filter {

    @Override
    public void init(FilterConfig config)
            throws ServletException {
        // No initialization required.
    }

    @Override
    public void doFilter(ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);

        if (session == null
                || session.getAttribute("ownerId") == null
                || session.getAttribute("pharmacyId") == null) {

            res.sendRedirect(
                    req.getContextPath() + "/owner-login.html");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup required.
    }
}