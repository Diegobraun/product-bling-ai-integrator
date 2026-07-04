package com.loja.catalogbling.repository;

import com.loja.catalogbling.domain.BlingToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlingTokenRepository extends JpaRepository<BlingToken, Long> {
}
