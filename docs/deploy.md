# Colocando o Pós-Créditos no ar

Este guia leva você do zero até o site funcionando na internet, **sem pressupor nenhum conhecimento prévio**. Siga as etapas na ordem. No fim de cada etapa há um "Como saber que deu certo". Se algo falhar, vá para [Problemas comuns](#problemas-comuns).

Tempo estimado: 2 a 3 horas na primeira vez, quase todo esperando páginas e cadastros.

## O que vamos montar

O site tem duas partes:

- **Front (o que o visitante vê):** as páginas do site. Fica na **Cloudflare**.
- **API (o "cérebro"):** guarda e entrega as reviews, confere a sua senha. Fica no **Google Cloud Run**.
- **Banco de dados:** onde as reviews ficam guardadas. Fica no **Neon**.

```
visitante ──► https://seudominio.com ──► Cloudflare Worker
                                          ├─ páginas do site (arquivos estáticos)
                                          └─ /api/*  ──► Google Cloud Run (a API)
                                                           └──► Neon (banco Postgres)

todo dia: GitHub Actions copia o banco ──► Cloudflare R2 (backup)
a cada alteração no código: GitHub Actions testa e publica tudo sozinho
```

O visitante só conhece `seudominio.com`. A API aparece como `seudominio.com/api/...`. Isso é de propósito: assim o login (que usa cookies) funciona sem configuração extra.

### Glossário rápido

| Palavra | O que é, em português simples |
|---|---|
| **Domínio** | O endereço do site (`seusite.com`). É a única coisa que se paga. |
| **DNS** | A "agenda telefônica" da internet: diz qual servidor atende cada domínio. |
| **Cloudflare** | Empresa que cuida do DNS, do HTTPS e hospeda o front (grátis). |
| **Worker** | Programa pequeno rodando na Cloudflare. O nosso entrega o site e repassa `/api/*` para a API. |
| **Google Cloud Run** | Serviço que roda a API. Liga quando chega uma visita e desliga quando ninguém usa (por isso não gasta parado). |
| **Neon** | Serviço que hospeda o banco de dados Postgres (grátis). |
| **Cloudflare R2** | "Pasta na nuvem" onde ficam os backups. |
| **GitHub Actions** | Robô do GitHub que roda os testes e faz o deploy quando você envia código. |
| **Secret** | Valor sigiloso (senha, chave). Guardado no GitHub, nunca no código. |
| **Variável** | Valor não sigiloso de configuração. |
| **Deploy** | Publicar uma nova versão no ar. |
| **Cold start** | A primeira visita depois de um tempo parado é mais lenta (10 a 25 segundos), porque a API precisa ligar. |

### Quanto custa

| Item | Custo | Observação |
|---|---|---|
| Domínio | cerca de US$ 10 por ano (`.com`) ou R$ 40 por ano (`.com.br`) | **Único custo obrigatório.** |
| Cloudflare (Workers, DNS, HTTPS) | grátis | Plano Free: 100 mil requisições por dia. |
| Google Cloud Run | grátis dentro da cota mensal | Cota: 2 milhões de requisições, 180 mil vCPU-segundos e 360 mil GiB-segundos por mês. Exige cartão de crédito cadastrado. |
| Artifact Registry (guarda a imagem da API) | grátis até cerca de 0,5 GB | O guia configura limpeza automática. |
| Neon (banco) | grátis | 1 GB de dados, 100 horas de computação por mês, sem cartão. |
| Cloudflare R2 (backups) | grátis até 10 GB | Pede um cartão cadastrado, mas não cobra dentro da cota. |
| GitHub Actions | grátis | Repositório público. |

Um site pessoal fica muito abaixo desses limites. Mesmo assim:

- **O Google Cloud não tem trava de gasto.** O alerta de orçamento (etapa 5) só avisa por e-mail. Se algo sair do controle, veja [Como desligar tudo](#como-desligar-tudo).
- A cota do Cloud Run é calculada em dinheiro, com preços da região mais barata. São Paulo é mais cara, então a cota rende um pouco menos lá. Continua sobrando para um site pessoal. Confira em https://cloud.google.com/run/pricing.
- **Não deixe um monitor ficar consultando a API** (veja a etapa 12): manteria o banco acordado o tempo todo e gastaria as 100 horas gratuitas do Neon.

## Antes de começar: a sua "folha de valores"

Ao longo do guia você vai gerar senhas e códigos. Abra um **gerenciador de senhas** (o Bitwarden é grátis) ou um arquivo de texto **fora da pasta do projeto** e vá anotando cada valor com o nome indicado. Na etapa 7 você vai colar todos no GitHub.

Regras de ouro:
- **Nunca** coloque senhas ou chaves dentro da pasta do projeto, em commits, prints ou conversas.
- Cada segredo tem uma finalidade. Não reutilize a mesma senha em lugares diferentes.

## Etapa 1: crie as contas

Você precisa de contas em: **GitHub** (você já tem, é onde está o código), **Cloudflare**, **Google Cloud**, **Neon** e **TMDB**. Use o mesmo e-mail, de preferência com verificação em duas etapas ligada.

1. Cloudflare: https://dash.cloudflare.com/sign-up
2. Google Cloud: https://console.cloud.google.com (entre com uma conta Google)
3. Neon: https://neon.com (pode entrar com a conta do GitHub)
4. TMDB: https://www.themoviedb.org/signup

## Etapa 2: domínio e Cloudflare

### 2.1 Registre o domínio

- **Para `.com` (mais simples):** na Cloudflare, menu **Domain Registration → Register Domains**, procure o nome, compre. A Cloudflare cobra o preço de custo, e o DNS já fica no lugar certo. Pule para a 2.3.
- **Para `.com.br`:** compre em https://registro.br (a Cloudflare não vende `.com.br`). Depois siga a 2.2.

### 2.2 Aponte um domínio comprado fora para a Cloudflare

(Só se você comprou em outro lugar.)

1. Na Cloudflare: **Add a domain**, digite o domínio, escolha o plano **Free**.
2. A Cloudflare mostra **dois nameservers** (algo como `ana.ns.cloudflare.com`).
3. No site onde você comprou o domínio, troque os nameservers pelos dois da Cloudflare.
4. Espere. Pode levar de minutos a algumas horas. A Cloudflare manda um e-mail quando o domínio estiver ativo.

### 2.3 Anote

- **Seu domínio**, por exemplo `seusite.com`. Daqui para frente o guia chama de `SEUDOMINIO`.
- **Account ID da Cloudflare:** abra a página do seu domínio na Cloudflare; no lado direito, em *API*, aparece **Account ID**. Anote como `CLOUDFLARE_ACCOUNT_ID`.

**Como saber que deu certo:** na Cloudflare, o domínio aparece como **Active**.

## Etapa 3: chave do TMDB

O TMDB fornece pôsteres, sinopses e dados dos filmes.

1. Entre em https://www.themoviedb.org/settings/api e peça uma chave de API (tipo "Developer" ou "Personal"). Preencha o formulário: nome do app (Pós-Créditos), a URL do seu site, e uma descrição curta ("site pessoal de reviews").
2. Quando aprovado, copie o **API Read Access Token** (é o código bem longo, não o "API Key" curto). Anote como `TMDB_ACCESS_TOKEN`.

## Etapa 4: banco de dados no Neon

1. Em https://console.neon.tech clique em **Create project**.
2. Nome: `pos-creditos`. **Postgres version: 17 ou 18** (a maior que aparecer). **Region: AWS South America (São Paulo)**. Se essa região não estiver disponível no plano grátis, escolha **AWS US East 1 (N. Virginia)** e, na etapa 5, use a região `us-east4` do Google Cloud.
3. Depois de criar, clique em **Connect**. **Desligue** a opção **Connection pooling** (isso é importante: a API precisa do endereço direto).
4. Ali aparecem os dados da conexão. Eles formam uma linha assim:

   ```
   postgresql://neondb_owner:SENHA@ep-nome-1234.sa-east-1.aws.neon.tech/neondb?sslmode=require
   ```

   Separe e anote:

   | Anote como | Exemplo | Onde está na linha |
   |---|---|---|
   | `DB_USERNAME` | `neondb_owner` | entre `postgresql://` e os dois pontos |
   | `DB_PASSWORD` | (a senha) | entre os dois pontos e o `@` |
   | `DB_HOST` | `ep-nome-1234.sa-east-1.aws.neon.tech` | entre o `@` e a `/` |
   | `DB_NAME` | `neondb` | entre a `/` e o `?` |

   O `DB_HOST` **não** pode ter `-pooler` no nome. Se tiver, o botão de pooling ainda está ligado.

**Como saber que deu certo:** no painel do Neon o projeto aparece com status ativo.

> O banco "dorme" depois de 5 minutos sem uso e acorda sozinho na próxima visita. Faz parte do plano grátis. Você não precisa criar tabelas: a API cria tudo na primeira vez que ligar.

## Etapa 5: Google Cloud

### 5.1 Crie o projeto

1. Em https://console.cloud.google.com, clique no seletor de projeto (no topo) e depois em **Novo projeto**.
2. Nome: `pos-creditos`. O Google gera um **ID do projeto** (por exemplo `pos-creditos-482913`). Anote como `GCP_PROJECT_ID`. Ele é único no mundo e não muda.

### 5.2 Ative o faturamento e crie um alerta de gasto

O Cloud Run exige um cartão de crédito cadastrado, mesmo dentro da cota gratuita.

1. Menu **Faturamento** (Billing) → vincule uma conta de faturamento ao projeto. Ao criar, o Google oferece créditos de teste. Quando o teste acabar, se o Google pedir para "ativar a conta completa", aceite: a cota gratuita continua valendo e o site continua no ar.
2. Ainda em **Faturamento**, abra **Orçamentos e alertas** (Budgets & alerts) → **Criar orçamento**. Escolha o projeto, valor **US$ 1**, mantenha os alertas de 50%, 90% e 100% por e-mail.

Lembre: o orçamento **só avisa**, não corta nada.

### 5.3 Abra o Cloud Shell

O Cloud Shell é um terminal que abre dentro do navegador, já com tudo instalado. Não precisa instalar nada no seu computador.

1. No topo da página do Google Cloud, clique no ícone **>_** (Ativar o Cloud Shell). Autorize se pedir.
2. Uma tela preta aparece embaixo. É ali que você cola os comandos. **Cole um bloco de cada vez** e espere terminar.

### 5.4 Execute os comandos de preparação

Bloco 1. Troque os dois valores entre aspas pelos seus (o projeto da etapa 5.1 e o seu usuário/repositório do GitHub):

```bash
export PROJECT_ID="COLE-AQUI-O-ID-DO-PROJETO"
export GITHUB_REPO="viniciuspadovam/movie-reviewer"
export REGION="southamerica-east1"
gcloud config set project "$PROJECT_ID"
```

Bloco 2. Liga os serviços do Google que vamos usar (leva 1 a 2 minutos):

```bash
gcloud services enable run.googleapis.com artifactregistry.googleapis.com \
  iam.googleapis.com iamcredentials.googleapis.com sts.googleapis.com \
  cloudresourcemanager.googleapis.com
```

Bloco 3. Cria o "armário" das imagens da API e uma regra para guardar só as 2 versões mais recentes (para nunca estourar a cota gratuita de armazenamento):

```bash
gcloud artifacts repositories create movie-review \
  --repository-format=docker --location="$REGION" --description="Imagens da API"

cat > cleanup.json <<'EOF'
[
  {"name": "keep-last-2", "action": {"type": "Keep"}, "mostRecentVersions": {"keepCount": 2}},
  {"name": "delete-the-rest", "action": {"type": "Delete"}, "condition": {"tagState": "any"}}
]
EOF

gcloud artifacts repositories set-cleanup-policies movie-review \
  --location="$REGION" --policy=cleanup.json --no-dry-run
```

Bloco 4. Cria duas "contas de robô": uma que **executa** a API (sem nenhuma permissão extra) e outra que o GitHub usa para **publicar**:

```bash
gcloud iam service-accounts create movie-review-runtime --display-name="Execucao da API"
gcloud iam service-accounts create github-deployer --display-name="Deploy pelo GitHub"

DEPLOYER="github-deployer@${PROJECT_ID}.iam.gserviceaccount.com"
RUNTIME="movie-review-runtime@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$DEPLOYER" --role="roles/run.admin"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$DEPLOYER" --role="roles/artifactregistry.writer"
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME" \
  --member="serviceAccount:$DEPLOYER" --role="roles/iam.serviceAccountUser"
```

Bloco 5. Permite que **somente o seu repositório** do GitHub publique no seu projeto, sem precisar guardar nenhuma chave do Google (é mais seguro que baixar um arquivo de chave):

```bash
gcloud iam workload-identity-pools create github \
  --location=global --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc movie-reviewer \
  --location=global --workload-identity-pool=github \
  --display-name="Repositorio movie-reviewer" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository == '${GITHUB_REPO}'"

PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"

gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/attribute.repository/${GITHUB_REPO}"

echo "GCP_PROJECT_NUMBER = $PROJECT_NUMBER"
```

A última linha mostra o **número do projeto**. Anote como `GCP_PROJECT_NUMBER`.

**Como saber que deu certo:** nenhum bloco terminou com a palavra `ERROR`. Se algum falhou, leia a mensagem: costuma ser um valor digitado errado no Bloco 1. Corrija e rode o bloco de novo (rodar duas vezes é seguro; mensagens "already exists" podem ser ignoradas).

## Etapa 6: gere as senhas e segredos

No seu computador, abra o **PowerShell** (não precisa ser como administrador). O **Docker Desktop** precisa estar aberto.

**Senha do administrador.** Troque `SUA-SENHA-FORTE` por uma senha de verdade (evite aspas e o acento grave `` ` ``). O comando gera o **hash** dela, que é a "impressão digital" da senha: guardamos o hash, nunca a senha.

```powershell
$saida = docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 x "SUA-SENHA-FORTE"
$hash = ($saida -split ':',2)[1].Trim()
$hash
$hash | Set-Clipboard
```

O hash começa com `$2y$10$` e tem 60 caracteres. Ele já foi copiado para a área de transferência. Anote como `ADMIN_PASSWORD_HASH`. Anote também o usuário que você quer usar (por exemplo `vinicius`) como `ADMIN_USERNAME`. Guarde a **senha** num gerenciador de senhas: é com ela que você entra no site.

**Dois códigos aleatórios** (um para assinar as sessões de login, outro para a API confiar no Worker):

```powershell
function Novo-Segredo { $b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b) }
Novo-Segredo
Novo-Segredo
```

O primeiro resultado vira `JWT_SECRET`; o segundo, `PROXY_SHARED_SECRET`. São textos de 64 caracteres. **Não** reutilize o mesmo para os dois.

## Etapa 7: cadastre tudo no GitHub

1. Abra o repositório no GitHub → **Settings** → **Secrets and variables** → **Actions**.
2. Aba **Secrets** → **New repository secret**. Crie um por linha da tabela abaixo (nome exatamente igual, valor da sua folha):

   | Nome do secret | Valor |
   |---|---|
   | `DB_HOST` | da etapa 4 |
   | `DB_NAME` | da etapa 4 |
   | `DB_USERNAME` | da etapa 4 |
   | `DB_PASSWORD` | da etapa 4 |
   | `ADMIN_USERNAME` | o usuário que você escolheu |
   | `ADMIN_PASSWORD_HASH` | o hash da etapa 6 |
   | `JWT_SECRET` | da etapa 6 |
   | `PROXY_SHARED_SECRET` | da etapa 6 |
   | `TMDB_ACCESS_TOKEN` | da etapa 3 |
   | `CLOUDFLARE_ACCOUNT_ID` | da etapa 2 |
   | `CLOUDFLARE_API_TOKEN` | você cria na etapa 9 (deixe para depois) |

   Os secrets do backup (`R2_*`) ficam para a etapa 11.

3. Aba **Variables** → **New repository variable**:

   | Nome da variável | Valor |
   |---|---|
   | `GCP_PROJECT_ID` | o ID do projeto (etapa 5.1) |
   | `GCP_PROJECT_NUMBER` | o número do projeto (etapa 5.4) |
   | `GCP_REGION` | `southamerica-east1` (ou `us-east4`, se você escolheu a Virgínia no Neon) |
   | `FRONTEND_ORIGIN` | `https://SEUDOMINIO` (com `https://`, sem barra no final e sem `www`) |

   Ainda **não** crie `API_ORIGIN` nem `DEPLOY_ENABLED`. Eles vêm nas próximas etapas.

**Como saber que deu certo:** a aba Secrets lista 10 secrets (o 11º, o token da Cloudflare, você cria na etapa 9) e a aba Variables lista 4 variáveis.

> `GCP_REGION` é o valor mais fácil de digitar errado. Confira letra por letra e use a mesma região do Bloco 1 da etapa 5.

## Etapa 8: primeiro deploy da API

1. Ainda em **Settings → Secrets and variables → Actions → Variables**, crie `DEPLOY_ENABLED` com o valor `true`. Isso liga a publicação automática.
2. Vá na aba **Actions** do repositório → workflow **backend** → botão **Run workflow** → branch `main` → **Run workflow**.
3. Espere de 6 a 10 minutos. Ele roda os testes, monta a imagem, envia para o Google e publica.
4. Quando terminar com ✅, clique no run e abra o resumo (*Summary*). Aparece **"API no ar"** com um endereço parecido com `https://movie-review-api-123456789.southamerica-east1.run.app`.
5. Copie esse endereço (sem barra no final) e crie a variável de repositório `API_ORIGIN` com ele.

**Como saber que deu certo:** abra o endereço seguido de `/actuator/health` no navegador. Deve aparecer `{"groups":["liveness","readiness"],"status":"UP"}`. Pode demorar uns 20 segundos na primeira vez (é o cold start).

Se o run ficou ❌, abra o passo que falhou e leia a mensagem em vermelho. As mais comuns estão em [Problemas comuns](#problemas-comuns).

## Etapa 9: publique o site na Cloudflare

### 9.1 Crie o token da Cloudflare

1. Na Cloudflare: ícone do perfil (canto superior direito) → **My Profile** → **API Tokens** → **Create Token**.
2. Use o modelo **Edit Cloudflare Workers** → **Use template**.
3. Em *Account Resources*, escolha a sua conta. Em *Zone Resources*, escolha o seu domínio. **Continue** → **Create Token**.
4. Copie o token (aparece **uma única vez**). Cadastre no GitHub como o secret `CLOUDFLARE_API_TOKEN`.

### 9.2 Conecte o seu domínio ao Worker

1. No repositório, abra o arquivo `frontend/wrangler.jsonc`.
2. No final, descomente a linha de `routes` (tire as duas barras `//` do início) e troque `example.com` pelo seu domínio:

   ```jsonc
   "routes": [{ "pattern": "seusite.com", "custom_domain": true }]
   ```

3. Salve e envie para o GitHub (`git add`, `git commit`, `git push`). Isso dispara o workflow **frontend**, que roda os testes (uns 4 a 6 minutos) e publica o site.

Se preferir testar antes de usar o domínio, deixe a linha comentada: o site fica em `https://pos-creditos.SEU-SUBDOMINIO.workers.dev`. Nesse caso, ajuste a variável `FRONTEND_ORIGIN` para esse endereço e rode o workflow **backend** de novo (a API só aceita pedidos vindos do endereço configurado).

**Como saber que deu certo:** a aba Actions mostra o workflow **frontend** com ✅, e `https://SEUDOMINIO` abre o site. Sem reviews ainda, aparece "Nenhuma review publicada ainda".

## Etapa 10: teste tudo

No navegador:

1. Abra `https://SEUDOMINIO`. A página carrega.
2. Abra `https://SEUDOMINIO/login`. Entre com o usuário e a senha da etapa 6.
3. No painel, clique em **Nova review**, busque um filme (por exemplo "Clube da Luta"), escolha, escreva um texto, dê uma nota e clique em **Publicar**.
4. Volte para `https://SEUDOMINIO`: a review aparece. Clique em **Sair** e confira que ela continua visível para visitantes.
5. Confira o cookie de login: depois de entrar, pressione `F12` → aba **Application** (Aplicativo) → **Cookies**. O cookie `AUTH_TOKEN` deve ter **HttpOnly**, **Secure** e **SameSite = Strict** marcados.

No PowerShell (use `curl.exe`; o `curl` sozinho é outro comando):

```powershell
# deve responder 200 e uma lista (vazia ou não)
curl.exe -i https://SEUDOMINIO/api/v1/genres

# sem login, deve responder 401 ou 403 (nunca 200)
curl.exe -i -X POST https://SEUDOMINIO/api/v1/admin/titles
```

Teste o limite de tentativas: erre a senha **6 vezes seguidas** na tela de login. A sexta deve avisar "Muitas tentativas de login". Espere 15 minutos para tentar de novo (ou entre com a senha certa antes de errar 5 vezes).

## Etapa 11: backups automáticos (R2)

A API roda sem disco próprio. O Neon guarda um histórico das últimas 6 horas, mas isso não é backup. Todo dia, o GitHub copia o banco inteiro para o R2, guardando os últimos 30 dias.

1. Na Cloudflare, menu **Storage & databases → R2 object storage**. Na primeira vez a Cloudflare pede um cartão de crédito (é verificação; dentro de 10 GB não cobra). Clique em **Create bucket**, nome `movie-review-backups`.
2. Na página do R2 (visão geral) clique em **Manage API tokens** → **Create API token**. Permissão: **Object Read & Write**. Restrinja ao bucket `movie-review-backups`. Crie.
3. A Cloudflare mostra **Access Key ID** e **Secret Access Key** (uma única vez). No GitHub, cadastre estes secrets:

   | Secret | Valor |
   |---|---|
   | `R2_ACCOUNT_ID` | o mesmo Account ID da etapa 2 |
   | `R2_ACCESS_KEY_ID` | Access Key ID |
   | `R2_SECRET_ACCESS_KEY` | Secret Access Key |
   | `R2_BUCKET` | `movie-review-backups` |

4. Teste: aba **Actions** → workflow **backup** → **Run workflow**. Deve terminar com ✅ e o resumo mostra o nome do arquivo e o tamanho.
5. Confira no R2: o arquivo `moviereview-AAAA-MM-DDTHH-MM-SSZ.dump` aparece no bucket.

O workflow roda sozinho todo dia às 04:15 (horário de Brasília).

> O GitHub desliga workflows agendados se o repositório ficar 60 dias sem nenhuma atividade. Se isso acontecer, aparece um aviso na aba Actions: basta clicar para reativar. Um commit qualquer também reinicia a contagem.

## Etapa 12: monitoramento (opcional)

Para ser avisado por e-mail se o site cair:

1. Crie uma conta grátis em https://uptimerobot.com.
2. **Add New Monitor** → tipo **HTTP(s)** → URL `https://SEUDOMINIO/` → intervalo de 5 minutos → seu e-mail.

Monitore **só a página inicial**, que é estática. **Não** monitore endereços `/api/...`: cada verificação acordaria a API e o banco. A cada 5 minutos, eles nunca dormiriam, e as 100 horas gratuitas mensais do Neon acabariam em cerca de duas semanas.

## No dia a dia

### Publicar uma alteração no código
Faça `git push` na branch `main`. O GitHub testa e publica sozinho: alterou `backend/`, publica a API; alterou `frontend/`, publica o site.

### Trocar a senha do administrador
1. Gere um novo hash (etapa 6).
2. Atualize o secret `ADMIN_PASSWORD_HASH` no GitHub.
3. Rode o workflow **backend** (Run workflow). A nova senha vale quando terminar.

### Deslogar todo mundo / trocar a chave de sessão
Troque o secret `JWT_SECRET` por um novo valor e rode o workflow **backend**.

### Trocar o segredo entre Worker e API
Troque `PROXY_SHARED_SECRET` e rode, nesta ordem, **backend** e depois **frontend**. Por poucos minutos o limite de tentativas de login pode tratar todos como um só visitante.

### Voltar para a versão anterior da API
Console do Google Cloud → **Cloud Run** → serviço `movie-review-api` → aba **Revisões** → escolha a anterior → **Gerenciar tráfego** → 100% nela.

### Ver os erros da API
Console do Google Cloud → **Cloud Run** → `movie-review-api` → aba **Registros** (Logs). Os erros do Worker ficam na Cloudflare, em **Workers & Pages → pos-creditos → Logs**.

## Restaurar um backup

**Antes de qualquer coisa:** se o problema aconteceu nas últimas 6 horas, o Neon permite voltar no tempo (**Restore** no painel do projeto). É mais fácil que o backup.

Para restaurar a partir do R2:

1. No R2, abra o bucket, clique no arquivo `.dump` mais recente e baixe para uma pasta, por exemplo `C:\backup`.
2. No Neon, crie um **projeto novo** (ou uma branch) para receber os dados. Anote o host, usuário, senha e banco dele, como na etapa 4 (com o pooling desligado).
3. No PowerShell, na pasta onde está o arquivo (`cd C:\backup`):

   ```powershell
   docker run --rm -v "${PWD}:/backup" -e PGPASSWORD="SENHA-DO-BANCO-NOVO" -e PGSSLMODE=require postgres:18-alpine pg_restore -h HOST-DO-BANCO-NOVO -U USUARIO-DO-BANCO-NOVO -d NOME-DO-BANCO-NOVO --clean --if-exists --no-owner --no-acl --exit-on-error /backup/NOME-DO-ARQUIVO.dump
   ```

4. Para usar o banco restaurado, atualize no GitHub os secrets `DB_HOST`, `DB_NAME`, `DB_USERNAME` e `DB_PASSWORD` e rode o workflow **backend**.

Faça esse teste **uma vez, agora**, com o banco novo vazio. Só assim você sabe que o backup funciona.

## Problemas comuns

| O que você vê | Causa provável | O que fazer |
|---|---|---|
| Workflow backend falha com "Faltam secrets ou variáveis no GitHub: ..." | Algum secret não foi cadastrado ou o nome está errado | Confira a lista da etapa 7, letra por letra |
| "DB_HOST usa o endereço 'pooler' do Neon" | Você copiou o endereço com pooling ligado | Neon → **Connect** → desligue **Connection pooling** e copie o host de novo |
| "JWT_SECRET precisa ter pelo menos 32 caracteres" | O valor ficou incompleto ao copiar | Gere de novo (etapa 6) |
| "ADMIN_PASSWORD_HASH deveria começar com $2" | Você colou a senha em vez do hash | Use o resultado do comando da etapa 6 |
| Erro `Permission denied` ou `iam.serviceAccounts.actAs` no deploy | Permissões do Google ainda não propagaram, ou um comando da etapa 5.4 falhou | Espere 2 minutos e rode o workflow de novo. Se persistir, refaça o Bloco 4 e o Bloco 5 |
| Erro `workload identity` / `unable to exchange token` | `GCP_PROJECT_NUMBER` errado, ou o repositório digitado no Bloco 1 não é o mesmo do GitHub (maiúsculas e minúsculas contam) | Confira o número e o `GITHUB_REPO`, e refaça o Bloco 5 |
| Deploy ok, mas o teste de saúde falha | A API não conseguiu conectar no banco | Cloud Run → **Registros**. Confira `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` |
| O site abre, mas a lista de reviews fica em erro e o `F12 → Network` mostra `503 API não configurada` | O Worker não tem `API_ORIGIN` ou `PROXY_SHARED_SECRET` | Crie a variável `API_ORIGIN` e o secret, e rode o workflow **frontend** de novo |
| O login responde 403 "Invalid CORS request" | `FRONTEND_ORIGIN` diferente do endereço que você usa no navegador (por exemplo com `www` ou sem `https://`) | Corrija a variável e rode o workflow **backend** |
| "Muitas tentativas de login" logo na primeira tentativa | `PROXY_SHARED_SECRET` diferente no Worker e na API, então todos os visitantes parecem o mesmo | Rode **backend** e depois **frontend** de novo, com o mesmo secret |
| A primeira visita do dia demora 10 a 25 segundos | Cold start: a API e o banco estavam dormindo | É normal e esperado. As visitas seguintes são rápidas |
| `curl` no PowerShell dá erro estranho | No PowerShell, `curl` é outro comando | Use `curl.exe` |
| Erro de permissão no domínio ao publicar o Worker | O token da Cloudflare não cobre o seu domínio | Edite o token: em *Zone Resources* inclua o domínio e adicione as permissões **Zone → DNS → Edit** e **Zone → Workers Routes → Edit** |
| O backup falha com "Falta o secret ..." | Falta cadastrar algum `R2_*` | Etapa 11 |

## Como desligar tudo

- **Parar de gastar no Google Cloud agora:** Console → **Faturamento** → **Gerenciar contas de faturamento** → **Desvincular** (ou desativar o faturamento do projeto `pos-creditos`). A API sai do ar, o site continua abrindo sem reviews.
- **Apagar a API:** Console → **Cloud Run** → `movie-review-api` → **Excluir**.
- **Apagar o projeto inteiro:** Console → **IAM e administrador → Configurações** → **Encerrar** (o Google apaga depois de 30 dias).
- **Pausar a publicação automática:** apague a variável `DEPLOY_ENABLED` no GitHub.
- **Tirar o site do ar:** Cloudflare → **Workers & Pages** → `pos-creditos` → **Settings → Delete**.
