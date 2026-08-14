package br.com.ricardo.vagaradar.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class OAuthSecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AppSecurityProperties properties, PasswordEncoder passwordEncoder) {
        if (properties.adminUsername() == null || properties.adminUsername().isBlank()
                || properties.adminPassword() == null || properties.adminPassword().length() < 12) {
            throw new IllegalStateException(
                    "Defina APP_ADMIN_USERNAME e uma APP_ADMIN_PASSWORD com pelo menos 12 caracteres."
            );
        }

        return username -> {
            if (!properties.adminUsername().equals(username)) {
                throw new UsernameNotFoundException("Administrador não encontrado.");
            }
            return User.withUsername(properties.adminUsername())
                    .password(passwordEncoder.encode(properties.adminPassword()))
                    .roles("ADMIN")
                    .build();
        };
    }

    @Bean
    SecurityFilterChain oauthSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider,
            ObjectProvider<OAuth2AuthorizationRequestResolver> authorizationRequestResolverProvider
    ) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/login.html", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/favicon.svg", "/actuator/health").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .headers(headers -> headers
                        .contentSecurityPolicy(policy -> policy.policyDirectives(
                                "default-src 'self'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'; "
                                        + "script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'"
                        ))
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .httpStrictTransportSecurity(Customizer.withDefaults())
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(new LoginCsrfAccessDeniedHandler(csrfRepository))
                )
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        OAuth2AuthorizedClientService authorizedClientService = authorizedClientServiceProvider.getIfAvailable();
        OAuth2AuthorizationRequestResolver authorizationRequestResolver = authorizationRequestResolverProvider.getIfAvailable();
        if (authorizedClientService != null && authorizationRequestResolver != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(authorizationRequestResolver))
                    .authorizedClientService(authorizedClientService)
                    .defaultSuccessUrl("/", true)
            );
        }

        return http.build();
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
