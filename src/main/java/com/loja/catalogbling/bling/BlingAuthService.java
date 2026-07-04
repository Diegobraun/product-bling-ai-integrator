package com.loja.catalogbling.bling;

import com.loja.catalogbling.config.BlingProperties;
import com.loja.catalogbling.domain.BlingToken;
import com.loja.catalogbling.repository.BlingTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class BlingAuthService {

    private static final String TOKEN_URL = "https://api.bling.com.br/Api/v3/oauth/token";
    private static final String AUTH_URL = "https://www.bling.com.br/Api/v3/oauth/authorize";
    private static final long TOKEN_ID = 1L;
    private static final long MARGEM_RENOVACAO_SEGUNDOS = 300;
    private static final long EXPIRACAO_PADRAO_SEGUNDOS = 21600;

    private final RestClient http = RestClient.create();
    private final BlingTokenRepository repo;
    private final BlingProperties props;

    public BlingAuthService(BlingTokenRepository repo, BlingProperties props) {
        this.repo = repo;
        this.props = props;
    }

    public String urlDeAutorizacao(String state) {
        return AUTH_URL + "?response_type=code&client_id=" + props.clientId() + "&state=" + state;
    }

    public void trocarCodePorTokens(String code) {
        guardar(requisitarTokens("grant_type=authorization_code&code=" + code));
    }

    public synchronized String accessTokenValido() {
        BlingToken token = repo.findById(TOKEN_ID).orElse(null);
        boolean precisaRenovar = token == null || token.getAccessToken() == null
                || Instant.now().isAfter(token.getExpiraEm().minusSeconds(MARGEM_RENOVACAO_SEGUNDOS));
        if (precisaRenovar) {
            if (token == null || token.getRefreshToken() == null) {
                throw new IllegalStateException(
                        "Sem tokens do Bling. Autorize o app em /bling/conectar.");
            }
            guardar(requisitarTokens("grant_type=refresh_token&refresh_token=" + token.getRefreshToken()));
        }
        return repo.findById(TOKEN_ID).orElseThrow().getAccessToken();
    }

    public boolean conectado() {
        return repo.findById(TOKEN_ID).map(t -> t.getRefreshToken() != null).orElse(false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requisitarTokens(String corpo) {
        return http.post()
                .uri(TOKEN_URL)
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(corpo)
                .retrieve()
                .body(Map.class);
    }

    private void guardar(Map<String, Object> resposta) {
        BlingToken token = repo.findById(TOKEN_ID).orElseGet(BlingToken::new);
        token.setAccessToken((String) resposta.get("access_token"));
        token.setRefreshToken((String) resposta.get("refresh_token"));
        long expiresIn = ((Number) resposta.getOrDefault("expires_in", EXPIRACAO_PADRAO_SEGUNDOS)).longValue();
        token.setExpiraEm(Instant.now().plusSeconds(expiresIn));
        repo.save(token);
    }

    private String basicAuthHeader() {
        String credenciais = props.clientId() + ":" + props.clientSecret();
        return "Basic " + Base64.getEncoder().encodeToString(credenciais.getBytes());
    }
}
