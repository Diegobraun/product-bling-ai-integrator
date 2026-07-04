package com.loja.catalogbling.web;

import com.loja.catalogbling.bling.BlingAuthService;
import com.loja.catalogbling.imagem.ImageStorage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

@Controller
public class BlingOAuthController {

    private final BlingAuthService auth;
    private final ImageStorage storage;

    public BlingOAuthController(BlingAuthService auth, ImageStorage storage) {
        this.auth = auth;
        this.storage = storage;
    }

    @GetMapping("/bling/conectar")
    public String conectar() {
        return "redirect:" + auth.urlDeAutorizacao(UUID.randomUUID().toString());
    }

    @GetMapping("/bling/callback")
    public String callback(@RequestParam String code) {
        auth.trocarCodePorTokens(code);
        return "redirect:/produtos";
    }

    @GetMapping("/public/imagens/{nome}")
    @ResponseBody
    public ResponseEntity<Resource> imagem(@PathVariable String nome) throws Exception {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new ByteArrayResource(storage.ler(nome)));
    }
}
