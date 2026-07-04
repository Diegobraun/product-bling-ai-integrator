package com.loja.catalogbling.imagem;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageProcessingService {

    private static final int LADO_ALVO = 1024;
    private static final int LIMITE_BYTES = 200 * 1024;
    private static final double NITIDEZ_MINIMA = 100.0;
    private static final float QUALIDADE_MINIMA = 0.30f;
    private static final float QUALIDADE_MAXIMA = 0.95f;
    private static final int ITERACOES_COMPRESSAO = 8;

    public record Resultado(byte[] jpeg, int kb, int larguraOriginal, int alturaOriginal,
                            double nitidez, boolean upscaleNecessario, boolean nitidezOk) {}

    public Resultado processar(byte[] entrada) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(entrada));
        if (original == null) {
            throw new IOException("Não foi possível ler a imagem (formato não suportado?).");
        }
        int largura = original.getWidth();
        int altura = original.getHeight();

        double nitidez = varianciaLaplaciano(original);
        boolean upscale = largura < LADO_ALVO && altura < LADO_ALVO;

        byte[] jpeg = comprimirAte(encaixarEmQuadradoBranco(original), LIMITE_BYTES);

        return new Resultado(jpeg, jpeg.length / 1024, largura, altura,
                nitidez, upscale, nitidez >= NITIDEZ_MINIMA);
    }

    private BufferedImage encaixarEmQuadradoBranco(BufferedImage origem) throws IOException {
        BufferedImage redimensionada = Thumbnails.of(origem)
                .size(LADO_ALVO, LADO_ALVO)
                .keepAspectRatio(true)
                .asBufferedImage();

        BufferedImage canvas = new BufferedImage(LADO_ALVO, LADO_ALVO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, LADO_ALVO, LADO_ALVO);
        int x = (LADO_ALVO - redimensionada.getWidth()) / 2;
        int y = (LADO_ALVO - redimensionada.getHeight()) / 2;
        g.drawImage(redimensionada, x, y, null);
        g.dispose();
        return canvas;
    }

    private byte[] comprimirAte(BufferedImage imagem, int limiteBytes) throws IOException {
        float baixa = QUALIDADE_MINIMA;
        float alta = QUALIDADE_MAXIMA;
        byte[] melhor = escreverJpeg(imagem, baixa);

        for (int i = 0; i < ITERACOES_COMPRESSAO; i++) {
            float qualidade = (baixa + alta) / 2f;
            byte[] tentativa = escreverJpeg(imagem, qualidade);
            if (tentativa.length <= limiteBytes) {
                melhor = tentativa;
                baixa = qualidade;
            } else {
                alta = qualidade;
            }
        }
        if (melhor.length > limiteBytes) {
            melhor = escreverJpeg(imagem, QUALIDADE_MINIMA);
        }
        return melhor;
    }

    private byte[] escreverJpeg(BufferedImage imagem, float qualidade) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(qualidade);

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (var ios = new MemoryCacheImageOutputStream(saida)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(imagem, null, null), param);
        } finally {
            writer.dispose();
        }
        return saida.toByteArray();
    }

    private double varianciaLaplaciano(BufferedImage imagem) {
        int w = imagem.getWidth();
        int h = imagem.getHeight();
        double[][] cinza = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = imagem.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                cinza[y][x] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        double soma = 0;
        double somaQuadrados = 0;
        long n = 0;
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                double laplaciano = cinza[y - 1][x] + cinza[y + 1][x]
                        + cinza[y][x - 1] + cinza[y][x + 1]
                        - 4 * cinza[y][x];
                soma += laplaciano;
                somaQuadrados += laplaciano * laplaciano;
                n++;
            }
        }
        double media = soma / n;
        return somaQuadrados / n - media * media;
    }
}
