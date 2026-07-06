package com.loja.productbling.catalogo.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repo;
    @Autowired
    private TestEntityManager em;

    @Test
    void salvaEBuscaPorBlingId() {
        Product p = new Product();
        p.setBlingProductId("ABC");
        p.setTitulo("Mouse");
        repo.save(p);

        assertThat(repo.findFirstByBlingProductId("ABC")).isPresent();
        assertThat(repo.findFirstByBlingProductId("XYZ")).isEmpty();
    }

    @Test
    void persisteImagensEmCascataOrdenadasPorOrdem() {
        Product p = new Product();
        p.setTitulo("Mouse");
        p.adicionarImagem(new ProductImage(p, "http://x/b.jpg", 10, true, 1));
        p.adicionarImagem(new ProductImage(p, "http://x/a.jpg", 10, true, 0));
        Long id = repo.save(p).getId();
        em.flush();
        em.clear();

        Product recarregado = repo.findById(id).orElseThrow();
        assertThat(recarregado.getImagens()).hasSize(2);
        assertThat(recarregado.getImagens().get(0).getOrdem()).isEqualTo(0);
        assertThat(recarregado.getImagens().get(1).getOrdem()).isEqualTo(1);
    }

    @Test
    void listaTodosPorCriadoEmDesc() {
        repo.save(new Product());
        repo.save(new Product());
        assertThat(repo.findAllByOrderByCriadoEmDesc()).hasSize(2);
    }
}
