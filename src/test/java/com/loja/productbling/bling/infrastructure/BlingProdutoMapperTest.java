package com.loja.productbling.bling.infrastructure;

import com.loja.productbling.catalogo.domain.Product;
import com.loja.productbling.catalogo.domain.ProductStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BlingProdutoMapperTest {

    private final BlingProdutoMapper mapper = new BlingProdutoMapper();

    @Test
    void mapeiaCamposDoBling() {
        Map<String, Object> dados = Map.of(
                "nome", "Mouse X",
                "gtin", "789",
                "descricaoCurta", "curta",
                "descricaoComplementar", "longa",
                "preco", "199,90");

        Product p = mapper.paraProduto("42", dados);

        assertThat(p.getBlingProductId()).isEqualTo("42");
        assertThat(p.getTitulo()).isEqualTo("Mouse X");
        assertThat(p.getEan()).isEqualTo("789");
        assertThat(p.getDescricaoCurta()).isEqualTo("curta");
        assertThat(p.getPreco()).isEqualByComparingTo(new BigDecimal("199.90"));
        assertThat(p.getStatus()).isEqualTo(ProductStatus.PUBLICADO);
    }

    @Test
    void camposAusentesViramNull() {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", "  ");
        Product p = mapper.paraProduto("1", dados);
        assertThat(p.getTitulo()).isNull();
        assertThat(p.getEan()).isNull();
        assertThat(p.getPreco()).isNull();
    }

    @Test
    void precoInvalidoViraNull() {
        Product p = mapper.paraProduto("1", Map.of("preco", "abc"));
        assertThat(p.getPreco()).isNull();
    }
}
