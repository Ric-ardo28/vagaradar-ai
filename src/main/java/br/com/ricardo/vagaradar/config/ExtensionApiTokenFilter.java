package br.com.ricardo.vagaradar.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class ExtensionApiTokenFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final AppSecurityProperties properties;

    public ExtensionApiTokenFilter(AppSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/extensao/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tokenConfigurado = properties.extensionApiToken();
        if (tokenConfigurado == null || tokenConfigurado.isBlank()) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "A integração da extensão não está configurada.");
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String tokenRecebido = authorization != null && authorization.startsWith(PREFIXO_BEARER)
                ? authorization.substring(PREFIXO_BEARER.length())
                : "";
        if (!MessageDigest.isEqual(
                tokenConfigurado.getBytes(StandardCharsets.UTF_8), tokenRecebido.getBytes(StandardCharsets.UTF_8)
        )) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Token da extensão inválido.");
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "extensao", null, List.of(new SimpleGrantedAuthority("ROLE_EXTENSION"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
