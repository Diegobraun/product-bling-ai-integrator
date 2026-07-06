package com.loja.productbling.bling.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "bling_token")
public class BlingToken {

    @Id
    private Long id = 1L;

    @Column(columnDefinition = "text")
    private String accessToken;
    @Column(columnDefinition = "text")
    private String refreshToken;
    private Instant expiraEm = Instant.EPOCH;

    public Long getId() { return id; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String v) { this.accessToken = v; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String v) { this.refreshToken = v; }
    public Instant getExpiraEm() { return expiraEm; }
    public void setExpiraEm(Instant v) { this.expiraEm = v; }
}
