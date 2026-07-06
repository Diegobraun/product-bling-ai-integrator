package com.loja.productbling.imagem.application;

import com.loja.productbling.imagem.domain.RenderizadorPagina;
import com.loja.productbling.support.FakeObjectProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ImageDownloadServiceExtracaoTest {

    private final ImageDownloadService service =
            new ImageDownloadService(new FakeObjectProvider<RenderizadorPagina>(null));

    private static final String HTML = """
        <html><head>
        <meta property="og:image" content="https://cdn.example.com/product/main-large.jpg">
        <script type="application/ld+json">
        {"image":["//images.samsung.com/is/image/samsung/p6/gallery/foo",
                  "https://images.kabum.com.br/produtos/fotos/123/mouse_p.jpg"]}
        </script>
        </head><body>
        <img src="https://static.kabum.com.br/conteudo/temas/banner_promo.png">
        <img src="https://cdn.example.com/media/mouse-preto.jpg">
        </body></html>
        """;

    private List<String> extrair() {
        List<String> urls = new ArrayList<>();
        service.extrairDeHtml(HTML, urls);
        return urls;
    }

    @Test
    void capturaOgImageJsonLdEScene7() {
        assertThat(extrair())
                .contains("https://cdn.example.com/product/main-large.jpg")
                .anyMatch(u -> u.startsWith("https://images.samsung.com/is/image/"));
    }

    @Test
    void normalizaProtocolRelativeParaHttps() {
        assertThat(extrair()).noneMatch(u -> u.startsWith("//"));
    }

    @Test
    void aplicaUpgradeDeResolucaoDaKabum() {
        assertThat(extrair())
                .contains("https://images.kabum.com.br/produtos/fotos/123/mouse_g.jpg")
                .doesNotContain("https://images.kabum.com.br/produtos/fotos/123/mouse_p.jpg");
    }

    @Test
    void filtroLixoRemoveBanners() {
        List<String> filtradas = service.filtrarCor(extrair(), Set.of());
        assertThat(filtradas).noneMatch(u -> u.contains("banner"));
    }

    @Test
    void filtroDeCorRemoveOutrasCores() {
        List<String> filtradas = service.filtrarCor(extrair(), Set.of("preto"));
        assertThat(filtradas).noneMatch(u -> u.contains("mouse-preto"));
    }
}
