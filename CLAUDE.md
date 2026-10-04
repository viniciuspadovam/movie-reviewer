# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projeto

**Pós-Créditos**: site pessoal de reviews de filmes e séries. Leitura pública; escrita só pelo dono (um único usuário). Uma obra (filme/série, importada do TMDB) tem 1..N reviews, uma por vez que foi assistida (sessões), e cada review pode ser editada. Restrição forte: **custo zero** além do domínio. [plan.md](plan.md) é o plano original; [docs/business-rules.md](docs/business-rules.md) tem as regras de negócio vigentes e [docs/deploy.md](docs/deploy.md) o passo a passo de produção.

## Arquitetura (monorepo)

- `backend/`: Spring Boot 4.1, Java 25, Maven, PostgreSQL 18 com Flyway (`db/migration`). Pacotes por domínio em `com.moviereview`: `title` (obras, gêneros, importação), `review` (CRUD admin + leituras públicas), `search` (busca com SQL nativo via `JdbcClient`), `tmdb` (cliente HTTP), `security`, `common` (exceções, `GlobalExceptionHandler`, slug, excerpt, paginação), `config`.
  - `TitleMetadataSource` é a porta do domínio; `TmdbClient` a implementa. O pacote `title` não conhece o TMDB.
  - Leituras públicas ficam em `ReviewQueryService` (numeração das sessões, variação da nota, nota atual = sessão publicada mais recente); escrita admin fica em `ReviewService`. Regras da review (nota 1–10, data não futura, texto obrigatório para publicar, `publishedAt` só na primeira publicação) ficam na entidade `Review`.
  - Busca: colunas `search_vector` geradas (`to_tsvector('portuguese', immutable_unaccent(...))`) em `title` e `review`, mais `ILIKE` sem acento no nome. Rascunhos nunca entram na busca, na nota atual nem na ordenação.
- `frontend/`: Angular 22 (standalone, zoneless, signals), Vitest, CSS próprio. `core/` (API clients, interceptor, auth, tema, formatação), `shared/` (componentes de apresentação), `features/public|auth|admin`. Publicado como static assets no Cloudflare Workers (`wrangler.jsonc`, modo SPA).
- `infra/`: `docker-compose.dev.yml` (Postgres local), `docker-compose.yml` (produção: postgres + api + cloudflared, sem portas publicadas), `backup.sh`/`restore.sh` (R2 via rclone), `.env.example`.
- `.github/workflows/`: `backend.yml` (testes → imagem arm64 no GHCR → deploy por SSH) e `frontend.yml` (Prettier + unitários + E2E → deploy no Workers). Imagem e deploy só rodam com a variável de repositório `DEPLOY_ENABLED=true`.

Produção: API na VM Oracle Always Free (ARM64), exposta só pelo Cloudflare Tunnel em `api.<dominio>`; front em `<dominio>`. Os dois precisam ficar no mesmo site por causa dos cookies.

## Decisões que atravessam camadas

- **Autenticação:** sem tabela de usuários. Usuário e hash BCrypt vêm de env. Login gera JWT (HS256) em cookie `AUTH_TOKEN` (`HttpOnly; Secure; SameSite=Strict`, `Domain=COOKIE_DOMAIN`). Só `/api/v1/admin/**` e `/auth/me` leem o cookie, para que um token vencido não quebre páginas públicas. Rate limit de login com Bucket4j por IP (`CF-Connecting-IP`).
- **CSRF:** double-submit. O backend emite `XSRF-TOKEN` (legível por JS, `Domain=.<dominio>`) em toda resposta. Como o `HttpClient` do Angular **não envia XSRF para URLs absolutas**, o `core/api-interceptor.ts` lê o cookie e manda `X-XSRF-TOKEN` (e `withCredentials`) nas escritas para a API.
- **Sessão no front:** visitantes não fazem a checagem `/auth/me`. O `Auth` só checa a sessão no navegador que já fez login (dica em `localStorage`), para não gerar 401 no console de todo visitante.
- **Nota:** 0,5 a 5 estrelas na UI, inteiro 1–10 no banco e na API. Só a UI converte (`starsLabel`); nunca use float.
- **Markdown:** renderizado com `marked` e passado por `[innerHTML]`; o sanitizador do Angular remove scripts, handlers e `javascript:`.
- **Identidade visual:** cinema clássico (sala escura como tema padrão, "jornal da manhã" como tema claro), acentos vermelhos, Newsreader + Archivo. Cores, fontes e espaçamentos só via tokens em `frontend/src/styles/tokens.css` (ver `docs/visual-identity.md`).
- **Idioma voltado ao usuário:** textos da UI e `ProblemDetail.detail` da API em português. Rotas do front em inglês: `/`, `/title/:slug`, `/review/:id`, `/search`, `/login`, `/admin`, `/admin/new`, `/admin/titles/:titleId/reviews/new`, `/admin/reviews/:id/edit`.

## Convenções de código

- **Idioma:** todo o código em inglês: classes, métodos, variáveis, pacotes, tabelas/colunas, enums (`DRAFT | PUBLISHED`), endpoints e nomes de arquivos. **Exceção: mensagens de log são em português.** Conteúdo de `docs/` é em português, com nomes de arquivo em inglês.
- **Sem Javadoc/JSDoc** (`/** ... */`). Nomes claros substituem documentação. Comentário de linha só quando explica um *porquê* não óbvio.
- **SOLID:** controller só faz HTTP, service tem a regra de aplicação, entidade guarda as próprias invariantes, repository faz o acesso a dados. Dependa de abstrações onde há implementações trocáveis (ex.: `TitleMetadataSource`).
- **DRY:** reutilize `SlugGenerator`, `MarkdownExcerpt`, `PageResponse`, `GlobalExceptionHandler` e, no front, os helpers de `core/format.ts` (`kindAndYear`, `nameWithYear`, `starsLabel`…). Não duplique validação entre camadas sem motivo.
- **KISS / YAGNI:** implemente só o que a v1 pede. Sem abstrações especulativas.
- Backend: entidades JPA nunca saem pela API (use DTOs `record`); injeção por construtor. Com `open-in-view` desligado, services devolvem DTOs montados **dentro** da transação.
- Não use `@Validated` em controllers: constraints em `@RequestParam`/`@PathVariable` já são validadas pelo Spring MVC e viram 400 em português no `GlobalExceptionHandler`. Com `@Validated`, viram `ConstraintViolationException` e retornam 500.
- Frontend: componentes standalone com `OnPush`. Estado em signals (é zoneless: campo comum alterado depois de um `await` não atualiza a tela). Acesso à API só em `core/*-api.ts`. Em `rxResource`, **cheque `hasValue()` antes de `value()`**, porque `value()` lança exceção quando o resource está em erro. Não ponha `@if` no meio de uma frase no template: o Prettier quebra a linha e cria espaços indevidos. Use um helper.
- Ids no HTML precisam ser únicos na página: `#content` é o `<main>` do shell.

## Fluxo de git

- O repositório e o remoto (`origin` → `git@github.com:viniciuspadovam/movie-reviewer.git`, branch `main`) já estão configurados. Não rode `git init` nem `git remote add`.
- Desenvolva **por feature**, não por arquivo ou camada solta. Ao concluir cada feature, com os testes passando: `git add` dos arquivos da feature → `git commit -m "<mensagem descritiva em inglês>"` → `git push`.
- Mensagem no imperativo, explicando o que mudou e por quê. Não misture features diferentes no mesmo commit.

## Comandos

Ambiente: Windows com Java 25, Node 24 e Docker Desktop. Não há Maven global: use o wrapper (`.\mvnw.cmd` no PowerShell, `./mvnw` no Git Bash). Se `node`/`npx` não estiverem no PATH do Git Bash, use `export PATH="/c/Program Files/nodejs:$PATH"`.

- Banco local: `docker compose -f infra/docker-compose.dev.yml up -d` (Postgres 18 em `localhost:5432`; banco, usuário e senha `moviereview`). Se o daemon não responder, inicie o Docker Desktop.
- Backend (em `backend/`):
  - rodar: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (login `admin`/`admin`, Swagger em `/swagger-ui.html`). Sem o perfil `dev`, a aplicação exige `FRONTEND_ORIGIN`, `ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH`, `JWT_SECRET` (e `TMDB_ACCESS_TOKEN` para importar obras).
  - testes: `./mvnw verify` · um teste: `./mvnw test -Dtest=ClassName#method`
- Frontend (em `frontend/`):
  - `npx ng serve` (proxy de `/api` para `localhost:8080`) · `npx ng build` · `npx ng test --watch=false`
  - formatação: `npm run format:check` (o CI falha sem isso) · corrigir: `npx prettier --write "src/**/*.{ts,html,css}" "e2e/**/*.{ts,mjs}" playwright.config.ts`
  - E2E: `npm run e2e`. O Playwright sobe um stub do TMDB (`e2e/tmdb-stub.mjs`), o backend (perfil `dev` apontando para o stub) e o `ng serve`. Precisa do Postgres local rodando e das portas 8080, 4200 e 8089 livres.
- Produção: a API de produção é definida em `frontend/src/environments/environment.production.ts` (o CI troca o placeholder pela variável `API_BASE_URL`).

## Testes do backend

- Testes de integração usam Testcontainers (Docker precisa estar rodando). Anote a classe com `@IntegrationTest` (perfil `test` + Postgres + MockMvc + WireMock no lugar do TMDB, num único contexto Spring). Limpe as tabelas no `@BeforeEach` (`TRUNCATE review, title_genre, title, genre RESTART IDENTITY CASCADE`).
- **Não use** o `csrf()` do spring-security-test: ele troca o repositório de CSRF no contexto em cache e quebra outros testes. Use `TestRequests.xsrf()`, `TestRequests.fromNewIp()` (para não esbarrar no rate limit) e `TestRequests.adminSession(mockMvc)`.
- O `MockHttpServletResponse` não imprime `SameSite` de cookies criados via `addCookie`. Verifique com `cookie().sameSite(...)`.
