package com.loja.productbling.imagem.application;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ImageProcessingServiceTest {

    private final ImageProcessingService service = new ImageProcessingService();

    @Test
    void normalizaParaQuadrado1024EAbaixoDe200kb() throws Exception {
        byte[] entrada = imagemRuidosa(800, 600);

        ImageProcessingService.Resultado r = service.processar(entrada, true);

        BufferedImage saida = ImageIO.read(new ByteArrayInputStream(r.jpeg()));
        assertThat(saida.getWidth()).isEqualTo(1024);
        assertThat(saida.getHeight()).isEqualTo(1024);
        assertThat(r.kb()).isLessThanOrEqualTo(200);
    }

    @Test
    void imagemPequenaSinalizaUpscale() throws Exception {
        ImageProcessingService.Resultado r = service.processar(imagemRuidosa(120, 120), false);
        assertThat(r.upscaleNecessario()).isTrue();
    }

    @Test
    void imagemGrandeNaoPedeUpscale() throws Exception {
        ImageProcessingService.Resultado r = service.processar(imagemRuidosa(1600, 1600), false);
        assertThat(r.upscaleNecessario()).isFalse();
    }

    @Test
    void imagemNitidaPassaNoTesteDeNitidez() throws Exception {
        ImageProcessingService.Resultado r = service.processar(imagemRuidosa(1024, 1024), true);
        assertThat(r.nitidezOk()).isTrue();
    }

    @Test
    void imagemChapadaReprovaNaNitidez() throws Exception {
        ImageProcessingService.Resultado r = service.processar(imagemChapada(1024, 1024), true);
        assertThat(r.nitidezOk()).isFalse();
    }

    private byte[] imagemRuidosa(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random rnd = new Random(42);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, rnd.nextInt(0xFFFFFF));
            }
        }
        return paraPng(img);
    }

    private byte[] imagemChapada(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(new Color(200, 200, 200));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return paraPng(img);
    }

    private byte[] paraPng(BufferedImage img) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
