package com.loja.catalogbling.imagem;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImageDownloadService {

    private static final int LADO_MINIMO = 400;
    private static final int MAX_BYTES = 20 * 1024 * 1024;
    private static final Duration TIMEOUT_CONEXAO = Duration.ofSeconds(10);
    private static final Duration TIMEOUT_REQUISICAO = Duration.ofSeconds(30);

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT_CONEXAO)
            .build();

    private static final Pattern[] META_IMAGEM = {
            Pattern.compile("(?:property|name)=\"(?:og:image|twitter:image)\"[^>]*content=\"([^\"]+)\""),
            Pattern.compile("content=\"([^\"]+)\"[^>]*(?:property|name)=\"(?:og:image|twitter:image)\"")
    };

    public byte[] baixarMelhor(List<String> urls, List<String> paginas) {
        byte[] direta = baixarMelhor(urls);
        if (direta != null) {
            return direta;
        }
        return baixarMelhor(extrairImagensDasPaginas(paginas));
    }

    private List<String> extrairImagensDasPaginas(List<String> paginas) {
        List<String> urls = new ArrayList<>();
        if (paginas == null) {
            return urls;
        }
        for (String pagina : paginas) {
            try {
                byte[] corpo = baixar(pagina);
                if (corpo == null) {
                    continue;
                }
                String html = new String(corpo, StandardCharsets.UTF_8);
                for (Pattern padrao : META_IMAGEM) {
                    Matcher m = padrao.matcher(html);
                    while (m.find()) {
                        String url = m.group(1);
                        if (url.startsWith("http") && !urls.contains(url)) {
                            urls.add(url);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return urls;
    }

    public byte[] baixarMelhor(List<String> urls) {
        if (urls == null) {
            return null;
        }
        for (String url : urls) {
            try {
                byte[] bytes = baixar(url);
                if (bytes != null && imagemValida(bytes)) {
                    return bytes;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private byte[] baixar(String url) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT_REQUISICAO)
                .header("User-Agent", "Mozilla/5.0 (compatible; catalog-bling-ia/1.0)")
                .header("Accept", "image/*,*/*;q=0.8")
                .GET()
                .build();
        HttpResponse<byte[]> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofByteArray());
        if (resposta.statusCode() != 200) {
            return null;
        }
        byte[] corpo = resposta.body();
        if (corpo == null || corpo.length == 0 || corpo.length > MAX_BYTES) {
            return null;
        }
        return corpo;
    }

    private boolean imagemValida(byte[] bytes) {
        try {
            BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(bytes));
            return imagem != null && imagem.getWidth() >= LADO_MINIMO && imagem.getHeight() >= LADO_MINIMO;
        } catch (Exception e) {
            return false;
        }
    }
}
