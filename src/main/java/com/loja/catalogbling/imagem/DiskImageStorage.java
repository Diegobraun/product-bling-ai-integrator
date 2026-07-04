package com.loja.catalogbling.imagem;

import com.loja.catalogbling.config.AppProperties;
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
    public String salvar(Long productId, byte[] jpeg) throws IOException {
        Path dir = Paths.get(props.storageDir());
        Files.createDirectories(dir);
        Files.write(dir.resolve(productId + ".jpg"), jpeg);
        return props.publicBaseUrl().replaceAll("/+$", "") + "/public/imagens/" + productId + ".jpg";
    }

    @Override
    public byte[] ler(String nome) throws IOException {
        return Files.readAllBytes(Paths.get(props.storageDir()).resolve(nome));
    }
}
