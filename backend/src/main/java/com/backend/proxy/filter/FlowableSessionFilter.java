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

@Component
public class FlowableSessionFilter extends OncePerRequestFilter {

    public static final String FLOWABLE_SESSION_KEY = "FLOWABLE_SESSION_CONTEXT";
    private static final String BUSINESS_API_PREFIX = "/api/cursos";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith(BUSINESS_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false);
            boolean hasActiveSession = session != null;
            
            if (hasActiveSession) {
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
