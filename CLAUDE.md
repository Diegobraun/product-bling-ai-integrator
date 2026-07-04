# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`catalog-bling-ia-integration` — Spring Boot dashboard (Portuguese/pt-BR) that researches products on the web from just a name, drafts listing content with an AI provider (Claude, Gemini, or Ollama — configurable), processes product images, and creates or updates products in the Bling v3 ERP — with a human review loop before publishing. Code, comments, and UI are all written in Portuguese; keep that convention. The codebase intentionally has no code comments — don't add them.

## Commands

Requires JDK 21 and Maven.

```bash
mvn spring-boot:run     # run the app at http://localhost:8080
mvn package             # build
mvn test                # (there is no src/test yet)
```

Runtime needs env vars: `BLING_CLIENT_ID`, `BLING_CLIENT_SECRET`, `APP_PUBLIC_BASE_URL` (a publicly reachable URL — Bling downloads the product image from it; in dev use an ngrok tunnel), and the key for the selected AI provider (`ANTHROPIC_API_KEY` or `GEMINI_API_KEY`; Ollama needs no key). Provider selection: `IA_PROVIDER` (content: claude|gemini|ollama, default claude) and `IA_PESQUISA_PROVIDER` (web research: claude|gemini, default claude — Ollama has no web search). Defaults live in `src/main/resources/application.yml`; typed config records are in `config/` (`@ConfigurationProperties`).

Database is H2 on file (`./dados/catalogo`), auto-created via `ddl-auto=update`; H2 console at `/h2-console`. Swap the datasource block for PostgreSQL in production (snippet in README).

## Architecture

Root package: `com.loja.catalogbling`. The core is a state machine driven by `ProductPipelineService` (root package), which orchestrates one service per concern:

- **State machine**: `domain/ProductStatus` — `RASCUNHO → GERADO → EM_REVISAO ⟳ → APROVADO → PUBLICADO` (plus `ERRO_PUBLICACAO`; `PUBLICADO` can loop back to `EM_REVISAO` or re-publish to update Bling). Valid transitions are enforced in the enum (`exigirTransicao`); the human-in-the-loop gate is that only approved products reach Bling.
- **`ia/`**: provider-neutral core with two ports — `ChatIa` (chat completion with optional base64 image) and `PesquisaWebIa` (web-grounded research). `ConteudoIaService` (generate/revise listing content, replaying per-product `ConversationTurn` history) and `PesquisaProdutoIaService` (name-only research flow) are written once against the ports and parse model JSON via `RespostaJson` (strips fences, slices outer braces). Provider adapters are selected by `@ConditionalOnProperty` on `ia.provider` / `ia.pesquisa-provider` (claude is `matchIfMissing`): `ia/claude` (raw Messages API via `AnthropicMessagesClient`, server-side `web_search_20260209`/`web_fetch_20260209` tools with `pause_turn` continuation), `ia/gemini` (`generateContent` REST, research via `google_search` grounding tool), `ia/ollama` (local `/api/chat`, ChatIa only — no web search, so `PesquisaProdutoIaService` takes an `ObjectProvider` and fails with a clear message if no research provider bean exists). **All content templates, title patterns, category rules, and the research prompt live in `ia/ProductPrompts.java` — the single place to change the output standard, shared by all providers.**
- **`imagem/`**: `ImageProcessingService` normalizes to 1024×1024 on a white canvas, binary-searches JPEG quality to stay ≤200 KB, and scores sharpness (Laplacian variance, min 100.0). `ImageStorage` is the storage abstraction (swap point for S3/R2 in production); `DiskImageStorage` writes `{productId}.jpg` to `app.storage-dir` and returns the public URL Bling will fetch. `ImageDownloadService` downloads researched image URLs and picks the first valid one (≥400px each side).
- **`bling/`**: `BlingAuthService` does OAuth2 (authorize → callback → token exchange) with tokens persisted as a single `BlingToken` row (id=1), auto-refreshed 5 min before expiry. `BlingProductClient` creates (`POST /Api/v3/produtos`), searches (`GET /produtos?nome=` with GTIN-first matching), and updates (`PUT /produtos/{id}`); publishing is create-or-update in `ProductPipelineService.publicar`. The image goes as an external URL (`midia.imagens.externas[].link`), not an upload.
- **`web/`**: server-rendered Thymeleaf + htmx. Controller POST actions return Thymeleaf fragments (e.g. `"produtos/fragmentos/conteudo :: painel"`) that htmx swaps in place — when adding an action to the product page, follow this fragment-return pattern rather than full-page redirects. `ProductPipelineService.criarPorPesquisa` is deliberately non-transactional (web research can take minutes; must not hold a DB transaction).

## Known caveats (from README)

- Bling payload field names (`descricaoCurta`, `descricaoComplementar`, `gtin`, `midia.imagens.externas`) and search params should be verified against https://developer.bling.com.br/referencia before production use.
- Disk-based image storage + `APP_PUBLIC_BASE_URL` is a dev setup; production should serve images from a public bucket (implement `ImageStorage`).
