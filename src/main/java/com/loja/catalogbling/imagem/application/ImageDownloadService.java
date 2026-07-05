package com.loja.catalogbling.imagem.application;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImageDownloadService {

    private static final int LADO_MINIMO = 350;
    private static final int MAX_BYTES = 20 * 1024 * 1024;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final Duration TIMEOUT_CONEXAO = Duration.ofSeconds(10);
    private static final Duration TIMEOUT_REQUISICAO = Duration.ofSeconds(30);

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT_CONEXAO)
            .build();

    public record ImagemBaixada(byte[] bytes, String origem) {}

    private static final Pattern[] META_IMAGEM = {
            Pattern.compile("(?:property|name)=\"(?:og:image|twitter:image)\"[^>]*content=\"([^\"]+)\""),
            Pattern.compile("content=\"([^\"]+)\"[^>]*(?:property|name)=\"(?:og:image|twitter:image)\""),
            Pattern.compile("<link[^>]+rel=\"image_src\"[^>]+href=\"([^\"]+)\""),
            Pattern.compile("itemprop=\"image\"[^>]*content=\"([^\"]+)\""),
            Pattern.compile("\"image\"\\s*:\\s*\"(https?:[^\"]+?\\.(?:jpe?g|png|webp)[^\"]*)\""),
            Pattern.compile("\"image\"\\s*:\\s*\\[\\s*\"(https?:[^\"]+?\\.(?:jpe?g|png|webp)[^\"]*)\"")
    };

    private static final Pattern KABUM_PRODUTO =
            Pattern.compile("/produto/\\d+/[\\w\\-]+");

    private static final Pattern[] CDN_GALERIA = {
            Pattern.compile("https://images\\d*\\.kabum\\.com\\.br/produtos/fotos/[\\w/.\\-]+?\\.(?:jpe?g|png|webp)"),
            Pattern.compile("https://http2\\.mlstatic\\.com/D_[\\w/.\\-]+?\\.(?:jpe?g|png|webp)"),
            Pattern.compile("https://m\\.media-amazon\\.com/images/I/[\\w/.\\-]+?\\.(?:jpe?g|png|webp)"),
            Pattern.compile("https://[\\w.\\-]*mlcdn\\.com\\.br/[\\w/.\\-]+?\\.(?:jpe?g|png|webp)")
    };

    public List<ImagemBaixada> baixarCandidatas(List<String> urlsDiretas, List<String> paginas,
                                                int maxTotal, int maxPorPagina) {
        List<ImagemBaixada> candidatas = new ArrayList<>();
        Set<String> vistos = new HashSet<>();

        if (urlsDiretas != null) {
            for (String url : urlsDiretas) {
                if (candidatas.size() >= maxTotal) {
                    return candidatas;
                }
                byte[] bytes = baixarImagem(url);
                if (bytes != null && vistos.add(digest(bytes))) {
                    candidatas.add(new ImagemBaixada(bytes, "url:" + url));
                }
            }
        }

        if (paginas != null) {
            for (String pagina : paginas) {
                if (candidatas.size() >= maxTotal) {
                    break;
                }
                int daPagina = 0;
                for (String url : imagensDaPagina(pagina)) {
                    if (candidatas.size() >= maxTotal || daPagina >= maxPorPagina) {
                        break;
                    }
                    byte[] bytes = baixarImagem(url);
                    if (bytes != null && vistos.add(digest(bytes))) {
                        candidatas.add(new ImagemBaixada(bytes, pagina));
                        daPagina++;
                    }
                }
            }
        }
        return candidatas;
    }

    private String digest(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return bytes.length + ":" + java.util.Arrays.hashCode(bytes);
        }
    }

    public List<String> buscarPaginasProduto(String consulta, int maxPaginas) {
        List<String> paginas = new ArrayList<>();
        if (consulta == null || consulta.isBlank()) {
            return paginas;
        }
        try {
            String url = "https://www.kabum.com.br/busca/"
                    + URLEncoder.encode(consulta.trim(), StandardCharsets.UTF_8);
            byte[] corpo = baixar(url);
            if (corpo == null) {
                return paginas;
            }
            String html = new String(corpo, StandardCharsets.UTF_8);
            Matcher m = KABUM_PRODUTO.matcher(html);
            while (m.find() && paginas.size() < maxPaginas) {
                String pagina = "https://www.kabum.com.br" + m.group();
                if (!paginas.contains(pagina)) {
                    paginas.add(pagina);
                }
            }
        } catch (Exception ignored) {
        }
        return paginas;
    }

    private List<String> imagensDaPagina(String pagina) {
        List<String> urls = new ArrayList<>();
        try {
            byte[] corpo = baixar(pagina);
            if (corpo == null) {
                return urls;
            }
            String html = new String(corpo, StandardCharsets.UTF_8);
            for (Pattern padrao : META_IMAGEM) {
                acumular(padrao, html, urls);
            }
            for (Pattern padrao : CDN_GALERIA) {
                acumular(padrao, html, urls);
            }
        } catch (Exception ignored) {
        }
        return urls;
    }

    private void acumular(Pattern padrao, String html, List<String> urls) {
        Matcher m = padrao.matcher(html);
        while (m.find()) {
            String bruto = (m.groupCount() >= 1 ? m.group(1) : m.group())
                    .replace("\\/", "/").replace("&amp;", "&");
            if (!bruto.startsWith("http")) {
                continue;
            }
            String url = melhorarResolucao(bruto);
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }
    }

    private String melhorarResolucao(String url) {
        if (url.contains("kabum.com.br")) {
            return url.replaceAll("_(?:p|m|b|t|pp)\\.(jpe?g|png|webp)", "_g.$1");
        }
        if (url.contains("mlstatic.com") && !url.contains("D_NQ_NP_2X_")) {
            return url.replace("D_NQ_NP_", "D_NQ_NP_2X_");
        }
        if (url.contains("m.media-amazon.com")) {
            return url.replaceAll("\\._[A-Z0-9,_]+_\\.(jpe?g|png|webp)", ".$1");
        }
        return url;
    }

    private byte[] baixarImagem(String url) {
        try {
            byte[] bytes = baixar(url);
            return bytes != null && imagemValida(bytes) ? bytes : null;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] baixar(String url) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT_REQUISICAO)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,image/jpeg,image/png,*/*;q=0.8")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
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
