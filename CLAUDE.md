# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado atual

O repositório ainda **não tem código**: só existe [plan.md](plan.md), que é a fonte de verdade do projeto (stack, modelo de dados, API, segurança, infra e ordem de implementação). Leia-o antes de qualquer trabalho. Atualize este arquivo com os comandos e a arquitetura reais assim que o esqueleto (backend/frontend/infra) existir.

## Projeto

Site pessoal de reviews de filmes e séries. Leitura pública; escrita só pelo dono (um único usuário). Uma obra (filme/série, importada do TMDB) tem 1..N reviews, uma por vez que foi assistida ("revisões"), e cada review pode ser editada. Restrição forte: **custo zero** além do domínio.

## Arquitetura planejada (monorepo)

- `backend/` — Spring Boot (Java 21+, Maven), PostgreSQL com Flyway. Pacotes por domínio: `config`, `security`, `title`, `review`, `tmdb`, `search`, `common`. API REST em `/api/v1`: `GET` público, `/api/v1/admin/**` autenticado, resto negado.
- `frontend/` — Angular (standalone + signals), publicado como static assets em Cloudflare Workers (`wrangler.jsonc`, modo SPA).
- `infra/` — `docker-compose.yml` (postgres + api + cloudflared), configuração do túnel, script de backup (`pg_dump` → Cloudflare R2).
- `docs/` — `visual-identity.md`, `business-rules.md`, ADRs (conteúdo em português, nomes de arquivo em inglês).
- `.github/workflows/` — `backend.yml` e `frontend.yml`.

Produção: API em VM Oracle Cloud Always Free (ARM64) exposta só via Cloudflare Tunnel em `api.<dominio>`; front em `<dominio>`. Front e API precisam ficar no **mesmo site** porque a autenticação depende disso.

## Decisões que atravessam várias camadas

- **Autenticação:** sem tabela de usuários. Usuário e hash da senha vêm de env (`ADMIN_USERNAME`, `ADMIN_PASSWORD_HASH`). Sessão via JWT em cookie `HttpOnly; Secure; SameSite=Strict` com `Domain=.<dominio>`. CSRF via `CookieCsrfTokenRepository` (Spring) + suporte XSRF nativo do `HttpClient` do Angular. Rate limit no login com Bucket4j, usando o IP de `CF-Connecting-IP`.
- **Nota:** de 0,5 a 5 estrelas na UI, armazenada como inteiro 1–10 (`smallint`). Converta nas bordas, nunca use float.
- **Visibilidade:** reviews `DRAFT` (o "RASCUNHO" do plano) nunca aparecem em endpoints públicos; só `/admin/reviews` as retorna.
- **TMDB:** a chave fica só no backend; o front acessa o TMDB pelo proxy `/admin/tmdb/search`. A atribuição ao TMDB no rodapé é obrigatória.
- **Busca:** full-text do Postgres (`tsvector` em português sobre nome + conteúdo). Os filtros do front ficam sincronizados com query params.
- **Markdown** das reviews precisa ser renderizado com sanitização (risco de XSS).
- **Identidade visual:** o site se chama **Pós-Créditos**. Direção: cinema clássico (fundo escuro quente, acentos vermelhos) com tipografia de jornal. Detalhes em `docs/visual-identity.md`. Valores de cor, fonte e espaçamento ficam em `frontend/src/styles/tokens.css`; não use valores soltos nos componentes. CSS próprio, sem Angular Material.
- **Idioma voltado ao usuário:** textos da UI e mensagens de erro da API (`ProblemDetail.detail`) em **português**. Rotas do front em inglês: `/`, `/title/:slug`, `/review/:id`, `/search`, `/login`, `/admin/**`.

## Convenções de código

- **Idioma:** todo o código em inglês: classes, métodos, variáveis, pacotes, tabelas/colunas, enums (`DRAFT | PUBLISHED`, não `RASCUNHO | PUBLICADA`), endpoints e nomes de arquivos. **Exceção: mensagens de log são em português.** Os termos em português no plan.md são descritivos; traduza-os ao implementar.
- **Sem Javadoc/JSDoc** (`/** ... */`). Nomes claros substituem documentação. Comentário de linha só quando explica um *porquê* não óbvio.
- **SOLID:** classes com responsabilidade única (controller só faz HTTP, service contém a regra de negócio, repository faz o acesso a dados); dependa de abstrações onde há implementações trocáveis (ex.: cliente TMDB atrás de uma interface, para o WireMock e os testes).
- **DRY:** extraia lógica repetida (conversão de nota, geração de slug, mapeamento entity↔DTO, tratamento de erro via `ProblemDetail` em um `@RestControllerAdvice` único). Não duplique validação entre camadas sem motivo.
- **KISS / YAGNI:** implemente só o que o plano pede para a v1. Sem abstrações especulativas.
- Entidades JPA nunca saem pela API: use DTOs (`record`). Injeção por construtor, sem `@Autowired` em campo.
- No Angular: componentes standalone, signals para estado, lógica de acesso à API em services, componentes de apresentação sem chamadas HTTP.

## Fluxo de git

- O repositório e o remoto (`origin` → `git@github.com:viniciuspadovam/movie-reviewer.git`, branch `main`) já estão configurados. Não rode `git init` nem `git remote add`.
- Desenvolva **por feature** (ex.: "review CRUD", "TMDB integration", "login rate limit"), não por arquivo ou camada solta.
- Ao concluir cada feature (com testes passando): `git add` dos arquivos da feature → `git commit -m "<mensagem descritiva em inglês>"` → `git push`.
- Mensagem no imperativo, explicando o que mudou e por quê (ex.: `Add review editing with updated_at tracking`).
- Não misture features diferentes no mesmo commit.

## Comandos (planejados; confirme quando o código existir)

- Banco local: `docker compose -f infra/docker-compose.dev.yml up -d` (Postgres 18 em `localhost:5432`, banco/usuário/senha `moviereview`). Se o daemon não responder, inicie o Docker Desktop.
- Ambiente de desenvolvimento: Windows com Java 25 e Docker. Não há Maven global (use o wrapper). No PowerShell, use `.\mvnw.cmd`; no Git Bash, `./mvnw`.
- Backend (em `backend/`): `./mvnw spring-boot:run` · testes + integração: `./mvnw verify` · um teste: `./mvnw test -Dtest=ClassName#method`
- Testes de integração usam Testcontainers (Docker precisa estar rodando); o TMDB é mockado com WireMock.
- Frontend: `ng serve` (com proxy para a API) · `ng test` · `ng build` · deploy: `wrangler deploy`
- E2E: Playwright (fluxo: login → importar obra → criar/editar reviews → ver como visitante).
- Imagem da API precisa ser `linux/arm64` (buildx), porque a VM é Ampere.
