package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.config.GmailPushProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "gmail.push", name = "enabled", havingValue = "true")
public class PubSubPushAuthenticationService {

    private final GmailPushProperties properties;
    private final JwtDecoder jwtDecoder;

    public PubSubPushAuthenticationService(GmailPushProperties properties) {
        this.properties = properties;
        this.jwtDecoder = JwtDecoders.fromIssuerLocation("https://accounts.google.com");
    }

    public void validar(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("A notificação não possui credencial do Pub/Sub.");
        }
        Jwt token = jwtDecoder.decode(authorizationHeader.substring("Bearer ".length()));
        if (!token.getAudience().contains(properties.audience())) {
            throw new IllegalArgumentException("A notificação possui audience inválida.");
        }
        if (!properties.serviceAccountEmail().equals(token.getClaimAsString("email"))
                || !Boolean.TRUE.equals(token.getClaim("email_verified"))) {
            throw new IllegalArgumentException("A notificação não foi enviada pela conta de serviço configurada.");
        }
    }
}
