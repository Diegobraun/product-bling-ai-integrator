# catalog-bling-ia-integration

Pipeline de cadastro de produtos para e-commerce: pesquisa com IA + Bling.

Dashboard web em Spring Boot que gera título, descrição curta e descrição
complementar de produtos com o Claude (seguindo seus templates + SEO), processa
a imagem (1024×1024, ≤200 KB) e publica no Bling v3 — com um ciclo de revisão
humano no meio: você edita o texto e/ou pede ajustes ao Claude antes de publicar.

Você pode partir só do **nome do produto**: o Claude pesquisa a ficha técnica e
as imagens na web (web search), baixa a melhor imagem e aplica o tratamento.
Na publicação, se o produto já existir no Bling (mesmo GTIN ou nome), ele é
**atualizado** (PUT) em vez de duplicado.

## Como rodar

Pré-requisitos: **JDK 21** e **Maven**.

1. Configure as variáveis de ambiente (ou edite `application.yml`):

   ```bash
   export ANTHROPIC_API_KEY=sk-ant-...   # se usar o provider claude (default)
   export BLING_CLIENT_ID=seu-client-id
   export BLING_CLIENT_SECRET=seu-client-secret
   # URL pública do app — o Bling BAIXA a imagem por ela.
   # Em dev, suba um túnel (ex.: ngrok http 8080) e use a URL https:
   export APP_PUBLIC_BASE_URL=https://seu-tunel.ngrok-free.app
   ```

2. Suba a aplicação:

   ```bash
   mvn spring-boot:run
   ```

3. Acesse http://localhost:8080

O banco padrão é **H2 em arquivo** (pasta `./dados`), então roda sem instalar
nada. Para produção, troque o bloco `spring.datasource` do `application.yml`
por PostgreSQL:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/catalogo
    username: postgres
    password: postgres
```

## Fluxo no dashboard

1. **+ Novo produto** — duas opções:
   - **Só o nome**: digite o nome (ex.: `PCCom Imperial AMD Ryzen 7 9800X3D /
     32 GB / SSD de 2 TB / RTX 5070 V3 / Windows 11 Home`) e o Claude pesquisa
     specs, marca, modelo, categoria, EAN e imagens em vários sites, baixa a
     melhor imagem e já processa. Pode levar alguns minutos.
   - **Manual**: cole os dados brutos do fornecedor, marca, modelo, categoria,
     EAN, preço e (opcional) a imagem.
2. Na tela do produto, **Processar imagem** deixa a foto em 1024×1024 e ≤200 KB,
   e mostra uma nota de nitidez.
3. **Gerar com o Claude** cria os três textos seguindo os templates e o SEO.
4. **Revise**: edite os campos direto, ou peça ajustes no chat
   ("deixe o título mais curto", "destaque o Dual Band"). O Claude mantém o
   contexto da conversa.
5. **Aprovar** libera a publicação. **Conectar Bling** (topo) faz o OAuth uma vez.
6. **Publicar no Bling** — se o produto já existir lá (id salvo, ou encontrado
   por GTIN/nome), atualiza via PUT; senão cria via POST. Produtos publicados
   têm o botão **Atualizar no Bling** para reenviar edições.

## Provedores de IA (Claude, Gemini, Ollama)

O provedor é escolhido no `application.yml` (ou por env), separadamente para a
geração de conteúdo e para a pesquisa web:

```yaml
ia:
  provider: claude           # claude | gemini | ollama   (geração de conteúdo)
  pesquisa-provider: claude  # claude | gemini            (pesquisa web)
```

| Provedor | Custo | Geração + visão | Pesquisa web | Configuração |
|---|---|---|---|---|
| **claude** (default) | pago (centavos/cadastro) | ✅ melhor escrita pt-BR | ✅ web search | `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL` (default `claude-sonnet-4-6`) |
| **gemini** | **free tier** | ✅ | ✅ Google Search grounding | `GEMINI_API_KEY` (crie grátis em https://aistudio.google.com), `GEMINI_MODEL` (default `gemini-2.5-flash`) |
| **ollama** | **grátis, 100% local** | ✅ (modelo multimodal) | ❌ não tem — use claude ou gemini na pesquisa | `ollama pull gemma3`; `OLLAMA_BASE_URL` (default `http://localhost:11434`), `OLLAMA_MODEL` (default `gemma3`) |

Setup 100% free: `IA_PROVIDER=gemini IA_PESQUISA_PROVIDER=gemini`.
Low cost máximo (geração local): `IA_PROVIDER=ollama IA_PESQUISA_PROVIDER=gemini`.

Atenção: no free tier do Gemini o Google pode usar os dados enviados para
treinamento — irrelevante para ficha técnica de produto, mas fique ciente.

## Onde ajustar os padrões

- Templates de descrição, padrão de título, regras de categoria e o prompt da
  pesquisa web: `ia/ProductPrompts.java` — é o único lugar que você mexe
  para mudar o padrão (vale para todos os provedores).
- Tamanho/limite da imagem: `imagem/ImageProcessingService.java`; critérios da
  imagem baixada da web (lado mínimo etc.): `imagem/ImageDownloadService.java`.

## Pontos a confirmar antes de produção

- **Campos do POST /produtos do Bling**: confira os nomes exatos
  (`descricaoCurta`, `descricaoComplementar`, `gtin`, `midia.imagens.externas`)
  na referência oficial: https://developer.bling.com.br/referencia
- **URL pública da imagem**: o Bling precisa alcançar `APP_PUBLIC_BASE_URL` pela
  internet no momento da publicação. Em produção, use um domínio real ou um
  bucket público (S3/R2/MinIO) — troque em `imagem/ImageStorageService.java`.
- **Segredos**: nunca commite chaves; use variáveis de ambiente.

## Estrutura

```
com.loja.catalogbling
├── config/      records @ConfigurationProperties (anthropic, gemini, ollama, bling, app)
├── domain/      entidades (Product, ProductStatus, ConversationTurn, BlingToken)
├── repository/  repositórios JPA
├── ia/          portas ChatIa + PesquisaWebIa, ProductPrompts,
│   │            ConteudoIaService + PesquisaProdutoIaService (neutros de provedor)
│   ├── claude/  AnthropicMessagesClient + ClaudeChat + ClaudePesquisaWeb
│   ├── gemini/  GeminiClient + GeminiChat + GeminiPesquisaWeb (Google Search grounding)
│   └── ollama/  OllamaChat (local, /api/chat)
├── imagem/      ImageProcessingService + ImageStorage (interface) / DiskImageStorage
│                + ImageDownloadService
├── bling/       BlingAuthService (OAuth + refresh) + BlingProductClient (criar/buscar/atualizar)
├── web/         controllers (produtos, OAuth Bling, serving de imagem)
└── ProductPipelineService  orquestra o fluxo e a máquina de estados
```
