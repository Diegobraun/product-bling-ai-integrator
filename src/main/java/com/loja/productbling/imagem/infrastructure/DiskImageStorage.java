package com.loja.productbling.imagem.infrastructure;

import com.loja.productbling.config.AppProperties;
import com.loja.productbling.imagem.domain.ImageStorage;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DiskImageStorage implements ImageStorage {

    private final AppProperties props;

    public DiskImageStorage(AppProperties props) {
        this.props = props;
    }

    @Override
    public String salvar(Long productId, int indice, byte[] jpeg) throws IOException {
        Path dir = Paths.get(props.storageDir());
        Files.createDirectories(dir);
        String nome = productId + "-" + indice + ".jpg";
        Files.write(dir.resolve(nome), jpeg);
        return props.publicBaseUrl().replaceAll("/+$", "") + "/public/imagens/" + nome;
    }

    @Override
    public byte[] ler(String nome) throws IOException {
        return Files.readAllBytes(Paths.get(props.storageDir()).resolve(nome));
    }

    @Override
    public void remover(String nome) throws IOException {
        if (nome != null) {
            Files.deleteIfExists(Paths.get(props.storageDir()).resolve(nome));
        }
    }
}
