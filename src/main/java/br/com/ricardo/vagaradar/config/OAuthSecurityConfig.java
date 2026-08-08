package br.com.ricardo.vagaradar.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(
        prefix = "gmail",
        name = "oauth-enabled",
        havingValue = "true"
)
public class OAuthSecurityConfig {

    @Bean
    SecurityFilterChain oauthSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/gmail/import"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/gmail/connect", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/gmail/alerts", "/api/gmail/import", "/api/gmail/connected").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl("/api/gmail/connected", true))
                .build();
    }
}
