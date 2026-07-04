package com.loja.catalogbling.repository;

import com.loja.catalogbling.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByCriadoEmDesc();
}
