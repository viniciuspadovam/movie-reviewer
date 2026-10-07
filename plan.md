# Plano — Site pessoal de reviews de filmes e séries

> **Atualização (outubro de 2026):** a hospedagem da API mudou. Em vez da VM Oracle com Cloudflare Tunnel, a API roda no **Google Cloud Run** com o banco no **Neon**, e o Worker da Cloudflare serve também `/api/*` na mesma origem. As seções "Stack decidida", "Fase 4" e "Verificação" abaixo descrevem o plano original. O que vale hoje está em `docs/deploy.md` e no `CLAUDE.md`.

## Contexto
Site pessoal, do zero (diretório `d:\workspace\movie-review` está vazio), para publicar reviews de filmes e séries.
- Leitura pública para qualquer visitante.
- Criação/edição/exclusão apenas pelo dono (um único usuário + senha).
- Uma obra pode ter **várias reviews** (uma por vez que foi assistida: "revisão"), e cada review pode ser **editada**.
- Extras da v1: **busca e filtros** e **nota numérica**. Metadados via **TMDB**.
- Restrições: **custo zero** (só o domínio), backend em **Java/Spring Boot**, frontend em **Angular** no **Cloudflare**.

## Stack decidida

| Camada | Escolha | Por quê |
|---|---|---|
| Frontend | Angular (última estável), standalone + signals, Angular Material ou CSS próprio com design tokens | Escolha do usuário |
| Hospedagem front | Cloudflare Workers (static assets, `not_found_handling = "single-page-application"`) | Grátis, CDN global |
| Backend | Spring Boot 3.x/4.x, Java 21+ (LTS), Maven | Escolha do usuário |
| Libs backend | Spring Web, Data JPA, Security, Validation, Flyway, springdoc-openapi, Bucket4j (rate limit), Testcontainers | Padrão de mercado |
| Banco | PostgreSQL 16+ em Docker na mesma VM | Grátis, sem limite de "pausa" de free tier |
| Hospedagem back | Oracle Cloud Always Free — VM ARM Ampere (região São Paulo) | Nunca dorme, sobra RAM para JVM + Postgres |
| Exposição/HTTPS | Cloudflare Tunnel (`cloudflared`) → `api.<dominio>` | Nenhuma porta HTTP aberta na VM, TLS automático |
| Backups | `pg_dump` diário (cron) → Cloudflare R2 (10 GB grátis) | Recuperação se a VM sumir |
| Metadados | TMDB API (chave fica só no backend) | Pôster, sinopse, ano, gêneros |
| CI/CD | GitHub Actions | Grátis para repo público/privado com cota |
| Domínio | Registrar um `.com`/`.com.br` com DNS na Cloudflare | Necessário para Tunnel + cookies same-site |

Observação: Cloudflare Workers não roda JVM, por isso o Spring fica na Oracle; o Cloudflare serve o Angular e faz o túnel/HTTPS da API.

## Estrutura do repositório (monorepo)
```
movie-review/
  backend/      # Spring Boot (Maven)
  frontend/     # Angular + wrangler.jsonc
  infra/        # docker-compose.yml, cloudflared config, scripts de backup, provisionamento da VM
  docs/         # identidade visual, regras de negócio, ADRs
  .github/workflows/  # backend.yml, frontend.yml
```

## Fase 0 — Identidade visual
- Definir nome do site, logo simples (SVG), paleta (tema escuro "sala de cinema" + tema claro), tipografia (Google Fonts: uma display serif/condensed para títulos + sans para texto), escala de espaçamento.
- Registrar tudo em `docs/identidade-visual.md` e como **design tokens** CSS (`frontend/src/styles/tokens.css`).
- Wireframes das telas: Home (últimas reviews), Página da obra (timeline de reviews), Página da review, Busca/filtros, Login, Painel admin (lista, editor).
- Usar a skill `frontend-design` nesta fase.

## Fase 1 — Regras de negócio (`docs/regras.md`)
1. **Obra** (filme ou série) é criada a partir do TMDB; um `tmdb_id` + tipo é único.
2. **Review** pertence a uma obra; uma obra tem 1..N reviews, ordenadas por data em que foi assistida. Cada review = uma "sessão" (1ª vez, revisão, etc.).
3. Campos da review: data em que assistiu, nota, texto (Markdown), flag "contém spoiler", status `RASCUNHO | PUBLICADA`.
4. **Nota**: 0,5 a 5 estrelas em passos de 0,5 (armazenada como inteiro 1–10). Página da obra mostra a nota da review mais recente e a evolução entre revisões.
5. **Edição** altera a review existente e atualiza `updated_at` (exibido como "editada em …"). Histórico de edições fica fora da v1.
6. **Exclusão** de review é definitiva (com confirmação). Excluir uma obra só é permitido sem reviews, ou exclui em cascata após confirmação explícita.
7. Visitantes só veem reviews `PUBLICADA`; rascunhos só o admin vê.
8. Spoilers ficam ocultos atrás de "mostrar" no front.
9. Exibir atribuição do TMDB no rodapé (exigência dos termos da API).

## Fase 2 — Backend (Spring Boot)

### Modelo de dados (Flyway `V1__init.sql`)
- `title` — id, tmdb_id, media_type (`MOVIE|SERIES`), name, original_name, release_year, overview, poster_path, backdrop_path, runtime/seasons, slug, created_at. Único `(tmdb_id, media_type)`.
- `genre` (id TMDB, nome) e `title_genre` (N:N).
- `review` — id, title_id (FK), watched_on, rating (smallint 1–10), content (text), has_spoilers, status, slug, created_at, updated_at.
- Índice full‑text (`tsvector` em português sobre nome + conteúdo) para a busca.

### Pacotes
`config`, `security`, `title`, `review`, `tmdb`, `search`, `common` (erros/ProblemDetail).

### API REST (`/api/v1`)
Público (GET):
- `/titles?q=&type=&genre=&minRating=&maxRating=&year=&sort=&page=` — busca + filtros, paginado
- `/titles/{slug}` — obra + reviews publicadas
- `/reviews/latest` · `/reviews/{id}`
- `/genres`

Admin (autenticado):
- `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`
- `GET /admin/tmdb/search?q=&type=` (proxy do TMDB)
- `POST /admin/titles` (a partir de `tmdbId`+tipo; busca detalhes e salva) · `DELETE /admin/titles/{id}`
- `POST /admin/titles/{id}/reviews` · `PUT /admin/reviews/{id}` · `DELETE /admin/reviews/{id}`
- `GET /admin/reviews?status=` (inclui rascunhos)

### Autenticação (um único usuário)
- Sem tabela de usuários: usuário e **hash BCrypt/Argon2** da senha vêm de variáveis de ambiente (`ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH`), nunca a senha em texto.
- Login gera sessão via **cookie HttpOnly + Secure + SameSite=Strict** (`Domain=.<dominio>`, front e API no mesmo site). Sessão stateless com JWT assinado (segredo em env) ou sessão Spring — preferir JWT em cookie para não depender de estado.
- **CSRF**: `CookieCsrfTokenRepository` do Spring + suporte nativo do `HttpClient` do Angular (`XSRF-TOKEN`/`X-XSRF-TOKEN`).
- **Rate limit** no login (Bucket4j: ex. 5 tentativas/15 min por IP, IP real vindo do header `CF-Connecting-IP`).
- CORS liberado só para a origem do front.
- Spring Security: `GET /api/v1/**` público, `/api/v1/admin/**` autenticado, resto negado.

### Testes
- Unitários dos serviços (regras de negócio: nota, status, slug).
- Integração com Testcontainers (Postgres) para repositórios, busca e segurança (401/403 em rotas admin, CSRF, rate limit).
- TMDB mockado (WireMock).

## Fase 3 — Frontend (Angular)
- Rotas públicas: `/`, `/obra/:slug`, `/review/:id`, `/buscar`.
- Rotas admin (`/admin/**`) com `authGuard` (consulta `/auth/me`): lista de reviews/rascunhos, "Nova obra" (busca TMDB → escolher → salvar), editor de review (Markdown com preview, seletor de estrelas, data, spoiler, status).
- Página da obra: cabeçalho com pôster/backdrop + **timeline de revisões** (cada review com data e nota, mostrando a evolução).
- Busca: campo de texto + filtros (tipo, gênero, faixa de nota, ano) sincronizados com query params.
- Interceptor HTTP: `withCredentials`, XSRF, tratamento de 401 → redireciona para login.
- Markdown renderizado com sanitização (ex. `ngx-markdown` + DomSanitizer) para evitar XSS.
- SEO básico: `Title`/`Meta` por página. SSR/pré-render pode vir depois (fase opcional).
- Testes: unitários (Vitest/Jasmine do CLI) + alguns E2E com Playwright (login, criar review, editar, ver como visitante).

## Fase 4 — Infra e deploy
1. **Oracle Cloud**: criar VM Ampere A1 (Ubuntu, ex. 2 OCPU/12 GB), só porta 22 aberta (chave SSH, sem senha), instalar Docker.
2. `infra/docker-compose.yml`: `postgres` (volume persistente), `api` (imagem ARM64 do GHCR), `cloudflared` (token do túnel). Rede interna; Postgres não exposto.
3. **Cloudflare**: domínio na Cloudflare; Tunnel apontando `api.<dominio>` → `http://api:8080`; Worker do front em `<dominio>`.
4. **Backup**: script `infra/backup.sh` (`pg_dump` → R2 via `rclone`), cron diário, retenção de 30 dias; testar restore.
5. **CI/CD (GitHub Actions)**:
   - `backend.yml`: testes → build da imagem `linux/arm64` (buildx) → push GHCR → SSH na VM → `docker compose pull && up -d`.
   - `frontend.yml`: lint/testes → `ng build` → `wrangler deploy`.
   - Segredos (DB, JWT, hash da senha, TMDB, tokens) em GitHub Secrets / `.env` na VM, fora do git.
6. Monitoramento simples: Spring Actuator `/health` + UptimeRobot (grátis) — também mantém evidência de que a VM está ativa (Oracle pode reclamar VMs ociosas do free tier).

## Ordem de implementação sugerida
0. Identidade visual + regras (docs) → 1. Esqueleto do monorepo + docker-compose local → 2. Backend: modelo, CRUD, segurança, TMDB, busca → 3. Frontend público → 4. Frontend admin → 5. Infra Oracle + Cloudflare + CI/CD → 6. Polimento (SEO, acessibilidade, performance).

## Verificação
- Local: `docker compose up` (Postgres) + `./mvnw spring-boot:run` + `ng serve` com proxy; `./mvnw verify` e `ng test` verdes.
- Segurança: sem login, `POST/PUT/DELETE` em `/api/v1/admin/**` retornam 401/403; login errado 6× seguidas retorna 429; cookie com `HttpOnly; Secure; SameSite=Strict`.
- Fluxo E2E (Playwright): login → adicionar obra pelo TMDB → criar review → criar segunda review (revisão) → editar a primeira → logout → como visitante, ver as duas na timeline e encontrar via busca/filtro de nota; rascunho não aparece.
- Produção: `https://<dominio>` carrega o front; `https://api.<dominio>/actuator/health` = UP; backup aparece no R2 e restore em banco limpo funciona.
