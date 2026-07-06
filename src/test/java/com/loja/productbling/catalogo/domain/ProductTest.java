package com.loja.productbling.catalogo.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void nasceComoRascunho() {
        assertThat(new Product().getStatus()).isEqualTo(ProductStatus.RASCUNHO);
    }

    @Test
    void transicaoValidaMudaOStatus() {
        Product p = new Product();
        p.transicionarPara(ProductStatus.GERADO);
        assertThat(p.getStatus()).isEqualTo(ProductStatus.GERADO);
    }

    @Test
    void transicaoInvalidaLanca() {
        Product p = new Product();
        assertThatThrownBy(() -> p.transicionarPara(ProductStatus.PUBLICADO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void proximaOrdemComecaEmZeroEIncrementa() {
        Product p = new Product();
        assertThat(p.proximaOrdem()).isEqualTo(0);
        p.adicionarImagem(new ProductImage(p, "http://x/0.jpg", 10, true, 0));
        p.adicionarImagem(new ProductImage(p, "http://x/1.jpg", 10, true, 1));
        assertThat(p.proximaOrdem()).isEqualTo(2);
    }

    @Test
    void imagemPrincipalEhAPrimeira() {
        Product p = new Product();
        assertThat(p.getImagemPrincipal()).isNull();
        ProductImage primeira = new ProductImage(p, "http://x/0.jpg", 10, true, 0);
        p.adicionarImagem(primeira);
        p.adicionarImagem(new ProductImage(p, "http://x/1.jpg", 10, true, 1));
        assertThat(p.getImagemPrincipal()).isSameAs(primeira);
    }

    @Test
    void adicionarTurnoRegistraConversa() {
        Product p = new Product();
        p.adicionarTurno("user", "oi");
        assertThat(p.getConversa()).hasSize(1);
        assertThat(p.getConversa().get(0).getPapel()).isEqualTo("user");
        assertThat(p.getConversa().get(0).getConteudo()).isEqualTo("oi");
    }

    @Test
    void removerImagemInexistenteRetornaFalse() {
        Product p = new Product();
        assertThat(p.removerImagem(999L)).isFalse();
    }
}
