package br.com.ricardo.vagaradar.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExtensionApiTokenFilterTest {

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAutenticarTokenValidoDaExtensao() throws Exception {
        ExtensionApiTokenFilter filter = filtro("token-de-extensao-seguro");
        MockHttpServletRequest request = requestComToken("token-de-extensao-seguro");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("extensao");
        verify(chain).doFilter(request, response);
    }

    @Test
    void deveRejeitarTokenInvalido() throws Exception {
        ExtensionApiTokenFilter filter = filtro("token-de-extensao-seguro");
        MockHttpServletRequest request = requestComToken("token-invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    private ExtensionApiTokenFilter filtro(String token) {
        return new ExtensionApiTokenFilter(new AppSecurityProperties("admin", "senha-de-teste-123", token));
    }

    private MockHttpServletRequest requestComToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/extensao/vagas");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
