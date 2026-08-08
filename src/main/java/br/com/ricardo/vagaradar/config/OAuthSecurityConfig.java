package br.com.ricardo.vagaradar.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(
        prefix = "gmail",
        name = "oauth-enabled",
        havingValue = "true"
)
public class OAuthSecurityConfig {

    @Bean
    SecurityFilterChain oauthSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizedClientService authorizedClientService,
            OAuth2AuthorizationRequestResolver authorizationRequestResolver
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/gmail/import", "/api/gmail/process"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/gmail/connect", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/gmail/alerts", "/api/gmail/import", "/api/gmail/process", "/api/gmail/connected").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(authorizationRequestResolver))
                        .authorizedClientService(authorizedClientService)
                        .defaultSuccessUrl("/api/gmail/connected", true)
                )
                .build();
    }
}
