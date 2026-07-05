package com.loja.catalogbling.bling.infrastructure;

import com.loja.catalogbling.catalogo.domain.Product;
import com.loja.catalogbling.catalogo.domain.ProductImage;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BlingProductClient {

    private static final String BASE = "https://api.bling.com.br/Api/v3";
    private static final int LIMITE_BUSCA = 50;

    private final RestClient http = RestClient.create();
    private final BlingAuthService auth;

    public BlingProductClient(BlingAuthService auth) {
        this.auth = auth;
    }

    @SuppressWarnings("unchecked")
    public String criarProduto(Product produto) {
        Map<String, Object> resposta = http.post()
                .uri(BASE + "/produtos")
                .header("Authorization", bearer())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(montarPayload(produto))
                .retrieve()
                .body(Map.class);
        return extrairId(resposta);
    }

    @SuppressWarnings("unchecked")
    public void atualizarProduto(String blingId, Product produto) {
        http.put()
                .uri(BASE + "/produtos/" + blingId)
                .header("Authorization", bearer())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(montarPayload(produto))
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listarProdutos(int pagina) {
        Map<String, Object> resposta = http.get()
                .uri(BASE + "/produtos?pagina=" + Math.max(1, pagina) + "&limite=" + LIMITE_BUSCA)
                .header("Authorization", bearer())
                .header("Accept", "application/json")
                .retrieve()
                .body(Map.class);

        Object data = resposta != null ? resposta.get("data") : null;
        List<Map<String, Object>> itens = new ArrayList<>();
        if (data instanceof List<?> lista) {
            for (Object item : lista) {
                if (item instanceof Map<?, ?> mapa) {
                    itens.add((Map<String, Object>) mapa);
                }
            }
        }
        return itens;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> obterProduto(String blingId) {
        Map<String, Object> resposta = http.get()
                .uri(BASE + "/produtos/" + blingId)
                .header("Authorization", bearer())
                .header("Accept", "application/json")
                .retrieve()
                .body(Map.class);

        Object data = resposta != null ? resposta.get("data") : null;
        return data instanceof Map<?, ?> mapa ? (Map<String, Object>) mapa : Map.of();
    }

    @SuppressWarnings("unchecked")
    public String buscarProdutoExistente(Product produto) {
        String nome = nomeDeBusca(produto);
        if (nome == null) {
            return null;
        }

        Map<String, Object> resposta = http.get()
                .uri(BASE + "/produtos?pagina=1&limite=" + LIMITE_BUSCA
                        + "&nome=" + URLEncoder.encode(nome, StandardCharsets.UTF_8))
                .header("Authorization", bearer())
                .header("Accept", "application/json")
                .retrieve()
                .body(Map.class);

        Object data = resposta != null ? resposta.get("data") : null;
        if (!(data instanceof List<?> itens)) {
            return null;
        }
        return encontrarCorrespondencia(itens, normalizar(produto.getEan()), normalizar(nome));
    }

    private String nomeDeBusca(Product produto) {
        if (produto.getTitulo() != null && !produto.getTitulo().isBlank()) {
            return produto.getTitulo();
        }
        if (produto.getModelo() != null && !produto.getModelo().isBlank()) {
            return produto.getModelo();
        }
        return null;
    }

    private String encontrarCorrespondencia(List<?> itens, String gtin, String nomeAlvo) {
        String idPorNome = null;
        for (Object item : itens) {
            if (!(item instanceof Map<?, ?> produto) || produto.get("id") == null) {
                continue;
            }
            String id = String.valueOf(produto.get("id"));
            if (gtin != null && gtin.equals(normalizar(texto(produto.get("gtin"))))) {
                return id;
            }
            if (idPorNome == null && nomeAlvo.equals(normalizar(texto(produto.get("nome"))))) {
                idPorNome = id;
            }
        }
        return idPorNome;
    }

    private Map<String, Object> montarPayload(Product produto) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("nome", produto.getTitulo());
        payload.put("tipo", "P");
        payload.put("situacao", "A");
        payload.put("formato", "S");
        if (produto.getEan() != null && !produto.getEan().isBlank()) {
            payload.put("gtin", produto.getEan());
        }
        if (produto.getPreco() != null) {
            payload.put("preco", produto.getPreco());
        }
        payload.put("descricaoCurta", produto.getDescricaoCurta());
        payload.put("descricaoComplementar", produto.getDescricaoComplementar());

        if (!produto.getImagens().isEmpty()) {
            List<Map<String, Object>> externas = new ArrayList<>();
            for (ProductImage imagem : produto.getImagens()) {
                externas.add(Map.of("link", imagem.getUrlPublica()));
            }
            payload.put("midia", Map.of(
                    "imagens", Map.of("externas", externas, "internas", List.of())));
        }
        return payload;
    }

    private String extrairId(Map<String, Object> resposta) {
        Object data = resposta != null ? resposta.get("data") : null;
        if (data instanceof Map<?, ?> m && m.get("id") != null) {
            return String.valueOf(m.get("id"));
        }
        throw new IllegalStateException("Resposta inesperada do Bling: " + resposta);
    }

    private String bearer() {
        return "Bearer " + auth.accessTokenValido();
    }

    private String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }

    private String normalizar(String texto) {
        return texto == null ? null : texto.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
