package com.loja.catalogbling.imagem.infrastructure;

import com.loja.catalogbling.imagem.domain.RenderizadorPagina;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "imagem.headless.enabled", havingValue = "true")
public class PlaywrightRenderizadorPagina implements RenderizadorPagina {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final double TIMEOUT_MS = 30_000;
    private static final double ESPERA_RENDER_MS = 2_500;

    @Override
    public synchronized String renderizar(String url) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            try {
                BrowserContext contexto = browser.newContext(
                        new Browser.NewContextOptions()
                                .setUserAgent(USER_AGENT)
                                .setLocale("pt-BR"));
                Page pagina = contexto.newPage();
                pagina.navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(TIMEOUT_MS));
                pagina.evaluate("() => window.scrollTo(0, document.body.scrollHeight)");
                pagina.waitForTimeout(ESPERA_RENDER_MS);
                return pagina.content();
            } finally {
                browser.close();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
