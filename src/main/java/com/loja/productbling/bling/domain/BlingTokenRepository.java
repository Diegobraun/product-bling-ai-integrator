package com.loja.productbling.bling.domain;

import com.loja.productbling.bling.domain.BlingToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlingTokenRepository extends JpaRepository<BlingToken, Long> {
}
