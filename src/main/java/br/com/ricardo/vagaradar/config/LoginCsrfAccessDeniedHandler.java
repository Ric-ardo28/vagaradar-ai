package br.com.ricardo.vagaradar.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.io.IOException;

final class LoginCsrfAccessDeniedHandler implements AccessDeniedHandler {

    private final CookieCsrfTokenRepository csrfTokenRepository;

    LoginCsrfAccessDeniedHandler(CookieCsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getRequestURI())) {
            csrfTokenRepository.saveToken(null, request, response);
            response.sendRedirect(request.getContextPath() + "/login?session=renewed");
            return;
        }

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
