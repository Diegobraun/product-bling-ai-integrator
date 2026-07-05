package com.loja.catalogbling.catalogo.domain;

import com.loja.catalogbling.catalogo.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByCriadoEmDesc();

    Optional<Product> findFirstByBlingProductId(String blingProductId);
}
