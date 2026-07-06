package com.loja.productbling.bling.infrastructure;

import com.loja.productbling.catalogo.domain.Product;
import com.loja.productbling.catalogo.domain.ProductStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class BlingProdutoMapper {

    public Product paraProduto(String blingId, Map<String, Object> dados) {
        Product produto = new Product();
        produto.setBlingProductId(blingId);
        produto.setTitulo(texto(dados.get("nome")));
        produto.setEan(texto(dados.get("gtin")));
        produto.setDescricaoCurta(texto(dados.get("descricaoCurta")));
        produto.setDescricaoComplementar(texto(dados.get("descricaoComplementar")));
        produto.setPreco(numero(dados.get("preco")));
        produto.setDadosBrutos("Produto importado do Bling (ID " + blingId + "). "
                + "Cole aqui a ficha técnica do fornecedor e clique em \"Gerar\" para reescrever o conteúdo.");
        produto.setStatus(ProductStatus.PUBLICADO);
        return produto;
    }

    private String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        String s = String.valueOf(valor).trim();
        return s.isEmpty() ? null : s;
    }

    private BigDecimal numero(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(valor).trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
