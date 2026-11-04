package com.backend.proxy.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.backend.proxy.context.FlowableSessionContext;
import com.backend.proxy.context.FlowableSessionHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Filtro en el Backend Proxy que recupera el FlowableSessionContext desde la sesión
 * del backend (HttpSession del usuario) y lo disponibiliza en FlowableSessionHolder
 * durante todo el ciclo de vida del request.
 */
@Component
public class FlowableSessionFilter extends OncePerRequestFilter {

    public static final String FLOWABLE_SESSION_KEY = "FLOWABLE_SESSION_CONTEXT";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false);

            if (session != null) {
                FlowableSessionContext context =
                    (FlowableSessionContext) session.getAttribute(FLOWABLE_SESSION_KEY);

                if (context != null) {
                    FlowableSessionHolder.set(context);
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            FlowableSessionHolder.clear();
        }
    }
}
