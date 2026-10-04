# Deploy em produção

Passo a passo para colocar o Pós-Créditos no ar com custo zero além do domínio. Cada etapa depende de uma conta sua; o código e os workflows já estão prontos.

Visão geral:

```
visitante ──https──► <dominio>        Cloudflare Workers (Angular estático)
          ──https──► api.<dominio>    Cloudflare Tunnel ──► VM Oracle (docker compose)
                                                            ├─ api (Spring Boot, ARM64)
                                                            ├─ postgres (volume local)
                                                            └─ cloudflared
backup diário: pg_dump ──► Cloudflare R2
```

## 1. Contas e domínio

1. Registre o domínio (`.com` ou `.com.br`) e coloque o DNS na Cloudflare (plano Free).
2. Crie uma conta no TMDB e gere o **API Read Access Token** em *Settings → API*.
3. Crie uma conta na Oracle Cloud com a região **Brazil East (São Paulo)** como região principal. A região não pode ser trocada depois e o Always Free só vale nela.

## 2. VM na Oracle

1. *Compute → Instances → Create*: imagem **Ubuntu 24.04**, shape **VM.Standard.A1.Flex** com 2 OCPU e 12 GB. Envie sua chave SSH pública.
2. Na *Security List* da VCN, mantenha **apenas a porta 22** de entrada. A API não precisa de porta aberta, porque o túnel é de saída.
3. Na VM:

   ```bash
   sudo apt update && sudo apt -y upgrade
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker "$USER"
   sudo mkdir -p /opt/movie-review && sudo chown "$USER" /opt/movie-review
   sudo apt -y install rclone
   # SSH só com chave
   sudo sed -i 's/^#\?PasswordAuthentication .*/PasswordAuthentication no/' /etc/ssh/sshd_config && sudo systemctl restart ssh
   ```

> A Oracle pode recuperar instâncias Always Free ociosas (CPU, rede e memória abaixo de 20% por 7 dias). Um monitor externo **não** conta como uso. A forma garantida de evitar isso é converter a conta para *Pay As You Go*: os recursos Always Free continuam gratuitos e as instâncias deixam de ser recuperadas. Configure um alerta de orçamento em US$ 1 para ter certeza de que nada é cobrado.

## 3. Cloudflare Tunnel

1. *Zero Trust → Networks → Tunnels → Create a tunnel* (tipo Cloudflared) e copie o **token**.
2. Em *Public Hostname*, adicione `api.<dominio>` → `HTTP` → `api:8080`. É o nome do serviço dentro da rede do compose.

## 4. Configuração na VM

1. Copie `infra/.env.example` para `/opt/movie-review/.env` e preencha. Valores com `$` (o hash da senha) **precisam** ficar entre aspas simples.
   - Hash da senha: `docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 "" 'sua-senha' | tr -d ':\n' | sed 's/^\$2y/\$2a/'`
   - Segredo do JWT: `openssl rand -base64 48`
   - `FRONTEND_ORIGIN=https://<dominio>` e `COOKIE_DOMAIN=.<dominio>`
2. A imagem da API vai para o GHCR. Se o pacote `movie-reviewer-api` ficar privado, faça login na VM com um token do GitHub que tenha apenas `read:packages`:
   `echo <token> | docker login ghcr.io -u viniciuspadovam --password-stdin`

## 5. GitHub Actions

Em *Settings → Secrets and variables → Actions* do repositório:

| Tipo | Nome | Valor |
|---|---|---|
| Secret | `VM_HOST` | IP público da VM |
| Secret | `VM_USER` | usuário SSH (ex.: `ubuntu`) |
| Secret | `VM_SSH_KEY` | chave privada SSH (crie um par só para o deploy) |
| Secret | `CLOUDFLARE_API_TOKEN` | token com permissão *Workers Scripts: Edit* |
| Secret | `CLOUDFLARE_ACCOUNT_ID` | ID da conta Cloudflare |
| Variable | `API_BASE_URL` | `https://api.<dominio>` |
| Variable | `DEPLOY_ENABLED` | `true` (ativa build da imagem e deploys) |

Crie também o *Environment* `production` (em *Settings → Environments*), opcionalmente exigindo aprovação manual.

No `frontend/wrangler.jsonc`, descomente `routes` com o seu domínio para o Worker responder em `<dominio>`.

Depois disso, um push em `main` que altere `backend/` testa, publica a imagem ARM64 e reinicia a API na VM. Um push que altere `frontend/` testa (unitários + E2E), gera o build e publica no Workers. Para o primeiro deploy, use *Actions → backend / frontend → Run workflow*.

## 6. Backups no R2

1. Na Cloudflare, crie o bucket `movie-review-backups` no R2 e um *API Token* do R2 com leitura e escrita nesse bucket.
2. Na VM, `rclone config` → novo remote `r2`, tipo *S3*, provider *Cloudflare*, com a chave, o segredo e o endpoint `https://<account-id>.r2.cloudflarestorage.com`.
3. Teste: `/opt/movie-review/backup.sh`.
4. Agende: `crontab -e` → `15 4 * * * /opt/movie-review/backup.sh >> /opt/movie-review/backup.log 2>&1`.
5. **Teste a restauração** pelo menos uma vez: `./restore.sh` lista os arquivos e `./restore.sh <arquivo>` restaura (pede confirmação e para a API durante o processo).

## 7. Monitoramento

No UptimeRobot (plano grátis), crie dois monitores HTTP(S):
- `https://api.<dominio>/actuator/health` (espera `"status":"UP"`)
- `https://<dominio>/`

## 8. Verificação final

- `https://<dominio>` abre o site e `https://api.<dominio>/actuator/health` responde `UP`.
- Sem login, `POST https://api.<dominio>/api/v1/admin/titles` retorna 401 ou 403.
- Seis logins errados seguidos retornam 429.
- No navegador, o cookie `AUTH_TOKEN` aparece com `HttpOnly`, `Secure` e `SameSite=Strict`.
- Um backup aparece no R2, e a restauração em um banco limpo funciona.
