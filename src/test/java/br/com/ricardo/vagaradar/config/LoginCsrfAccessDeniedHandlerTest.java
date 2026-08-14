package br.com.ricardo.vagaradar.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;

class LoginCsrfAccessDeniedHandlerTest {

    @Test
    void deveRenovarOCookieCsrfERedirecionarQuandoOFalhaAconteceNoLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LoginCsrfAccessDeniedHandler handler = new LoginCsrfAccessDeniedHandler(
                CookieCsrfTokenRepository.withHttpOnlyFalse()
        );

        handler.handle(request, response, new AccessDeniedException("CSRF inválido"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?session=renewed");
        assertThat(response.getCookie("XSRF-TOKEN").getMaxAge()).isZero();
    }

    @Test
    void deveManterForbiddenParaAcoesProtegidasForaDoLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/vagas");
        MockHttpServletResponse response = new MockHttpServletResponse();
        LoginCsrfAccessDeniedHandler handler = new LoginCsrfAccessDeniedHandler(
                CookieCsrfTokenRepository.withHttpOnlyFalse()
        );

        handler.handle(request, response, new AccessDeniedException("CSRF inválido"));

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
