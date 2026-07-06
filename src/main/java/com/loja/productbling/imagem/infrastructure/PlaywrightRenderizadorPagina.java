package com.loja.productbling.imagem.infrastructure;

import com.loja.productbling.config.HeadlessProperties;
import com.loja.productbling.imagem.domain.RenderizadorPagina;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "imagem.headless.enabled", havingValue = "true")
public class PlaywrightRenderizadorPagina implements RenderizadorPagina {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final String STEALTH = """
        Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
        window.chrome = { runtime: {}, app: {}, csi: () => {}, loadTimes: () => {} };
        Object.defineProperty(navigator, 'languages', { get: () => ['pt-BR', 'pt', 'en-US', 'en'] });
        Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });
        Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });
        Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });
        const permQuery = window.navigator.permissions.query;
        window.navigator.permissions.query = (p) =>
            p && p.name === 'notifications'
                ? Promise.resolve({ state: Notification.permission })
                : permQuery(p);
        const getParameter = WebGLRenderingContext.prototype.getParameter;
        WebGLRenderingContext.prototype.getParameter = function (parameter) {
            if (parameter === 37445) return 'Intel Inc.';
            if (parameter === 37446) return 'Intel Iris OpenGL Engine';
            return getParameter.call(this, parameter);
        };
        """;

    private final HeadlessProperties props;

    public PlaywrightRenderizadorPagina(HeadlessProperties props) {
        this.props = props;
    }

    @Override
    public synchronized String renderizar(String url) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(opcoesLancamento());
            try {
                BrowserContext contexto = browser.newContext(opcoesContexto());
                if (props.stealth()) {
                    contexto.addInitScript(STEALTH);
                }
                Page pagina = contexto.newPage();
                pagina.navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(props.timeoutMs()));
                pagina.waitForTimeout(props.esperaRenderMs());
                pagina.evaluate("() => window.scrollTo(0, document.body.scrollHeight)");
                pagina.waitForTimeout(props.esperaRenderMs());
                return pagina.content();
            } finally {
                browser.close();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private BrowserType.LaunchOptions opcoesLancamento() {
        List<String> args = new ArrayList<>(List.of(
                "--disable-blink-features=AutomationControlled",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-infobars",
                "--start-maximized"));
        BrowserType.LaunchOptions opcoes = new BrowserType.LaunchOptions()
                .setHeadless(!props.headed())
                .setArgs(args)
                .setIgnoreDefaultArgs(List.of("--enable-automation"));
        if (!"chromium".equalsIgnoreCase(props.channel())) {
            opcoes.setChannel(props.channel());
        }
        return opcoes;
    }

    private Browser.NewContextOptions opcoesContexto() {
        return new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setLocale("pt-BR")
                .setTimezoneId("America/Sao_Paulo")
                .setViewportSize(1920, 1080)
                .setDeviceScaleFactor(1)
                .setExtraHTTPHeaders(Map.of(
                        "Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8",
                        "sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
                        "sec-ch-ua-mobile", "?0",
                        "sec-ch-ua-platform", "\"macOS\"",
                        "Upgrade-Insecure-Requests", "1"));
    }
}
