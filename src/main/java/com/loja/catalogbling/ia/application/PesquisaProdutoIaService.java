package com.loja.catalogbling.ia.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.catalogbling.ia.domain.PesquisaDeProduto;
import com.loja.catalogbling.ia.domain.PesquisaEntrada;
import com.loja.catalogbling.ia.domain.PesquisaWebIa;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PesquisaProdutoIaService {

    private static final int MAX_TOKENS = 8000;

    private final ObjectProvider<PesquisaWebIa> pesquisaWeb;
    private final ObjectMapper json = new ObjectMapper();

    public PesquisaProdutoIaService(ObjectProvider<PesquisaWebIa> pesquisaWeb) {
        this.pesquisaWeb = pesquisaWeb;
    }

    public PesquisaDeProduto pesquisar(PesquisaEntrada entrada) {
        PesquisaWebIa ia = pesquisaWeb.getIfAvailable();
        if (ia == null) {
            throw new IllegalStateException(
                    "Nenhum provedor de pesquisa web configurado. "
                    + "Defina ia.pesquisa-provider como claude ou gemini (Ollama não tem pesquisa web).");
        }
        return parsear(ia.pesquisar(
                ProductPrompts.PESQUISA, ProductPrompts.mensagemPesquisa(entrada), MAX_TOKENS));
    }

    private PesquisaDeProduto parsear(String texto) {
        try {
            JsonNode n = json.readTree(RespostaJson.extrairObjeto(texto));
            return new PesquisaDeProduto(
                    textoOuNull(n, "marca"),
                    textoOuNull(n, "modelo"),
                    textoOuNull(n, "categoria"),
                    textoOuNull(n, "ean"),
                    textoOuNull(n, "dadosBrutos"),
                    urls(n, "imagens"),
                    urls(n, "paginas"),
                    texto);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Não consegui parsear a pesquisa da IA como JSON:\n" + texto, e);
        }
    }

    private List<String> urls(JsonNode n, String campo) {
        List<String> lista = new ArrayList<>();
        for (JsonNode item : n.path(campo)) {
            String url = item.asText("");
            if (url.startsWith("http")) {
                lista.add(url);
            }
        }
        return lista;
    }

    private String textoOuNull(JsonNode n, String campo) {
        JsonNode valor = n.path(campo);
        if (valor.isMissingNode() || valor.isNull()) {
            return null;
        }
        String texto = valor.asText().trim();
        return texto.isEmpty() || "null".equalsIgnoreCase(texto) ? null : texto;
    }
}
