# Deploy ativo na AWS

## Ambiente atual

O VagaRadar AI está hospedado em uma instância virtual Amazon Lightsail:

| Item | Configuração atual |
| --- | --- |
| Instância | `vagaradar-app` |
| Localização | São Paulo, Zone A (`sa-east-1a`) |
| Sistema operacional | Ubuntu |
| Plano | Lightsail General purpose — 2 vCPUs, 4 GB de RAM e 80 GB SSD |
| Rede | Dual-stack (IPv4 e IPv6) |
| Estado verificado em 18/08/2026 | Em execução |

O serviço público está disponível em:

- Painel: https://56.125.167.156.sslip.io/login
- Health check: https://56.125.167.156.sslip.io/actuator/health

Em 18 de agosto de 2026, o health check respondeu `UP` por HTTPS. O projeto não deve ser iniciado localmente para
uso normal enquanto esse ambiente remoto estiver ativo.

## Rede e acesso

O firewall da instância permite tráfego de entrada para:

| Porta | Finalidade | Origem atual |
| --- | --- | --- |
| TCP 80 | HTTP | Qualquer endereço IPv4 ou IPv6 |
| TCP 443 | HTTPS | Qualquer endereço IPv4 |
| TCP 22 | SSH | Qualquer endereço IPv4 ou IPv6, além do SSH no navegador Lightsail |

Não há balanceador ou CDN associado à instância. O endereço IPv4 público atual é `56.125.167.156`, mas ainda **não**
há um IP estático anexado: ele pode mudar se a instância for parada e iniciada. Como o domínio `sslip.io` depende desse
IP, a eventual mudança quebraria o acesso público e o redirecionamento OAuth até a atualização das configurações.

O acesso administrativo por SSH usa o usuário `ubuntu` e a chave-padrão da região São Paulo. A chave privada nunca
deve ser incluída no repositório. Como a porta 22 está aberta para qualquer origem, restrinja-a ao IP administrativo
confiável quando for possível, preservando uma forma segura de recuperação de acesso.

## Continuidade

Snapshots automáticos estão desativados e não foram identificados snapshots manuais no console. Antes de alterações
de infraestrutura ou atualização de sistema, crie um snapshot e mantenha um procedimento de recuperação testado.

## Operação segura

As credenciais e as variáveis de produção ficam exclusivamente no servidor ou no gerenciador de segredos. Não copie
nem versione `.env`, chaves de acesso, credenciais do banco, tokens OAuth, chave da OpenAI ou webhook do Discord.

O ambiente precisa manter, no mínimo:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
APP_ADMIN_USERNAME
APP_ADMIN_PASSWORD
OPENAI_API_KEY
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GMAIL_OAUTH_ENABLED=true
GMAIL_SCHEDULER_ENABLED=true
GMAIL_SCHEDULER_FIXED_DELAY=PT1H
GMAIL_SCHEDULER_INITIAL_DELAY=PT5M
DISCORD_WEBHOOK_URL
DISCORD_MINIMUM_SCORE=70
DISCORD_OUTBOX_ENABLED=true
DISCORD_OUTBOX_FIXED_DELAY=PT30S
DISCORD_OUTBOX_INITIAL_DELAY=PT10S
SESSION_COOKIE_SECURE=true
CADDY_DOMAIN=56.125.167.156.sslip.io
```

As vagas aprovadas na análise são gravadas primeiro na fila de saída do Discord. Um processamento em segundo plano
consulta essa fila a cada 30 segundos e tenta enviar cada alerta; uma falha temporária não perde a análise nem bloqueia
as demais vagas.

O PostgreSQL deve continuar inacessível pela internet. O endpoint público expõe somente o proxy HTTPS e a aplicação.

## Atualização e verificação

Antes de uma atualização, confirme que o repositório local está limpo e que os testes passam:

```bash
mvn test
```

No ambiente AWS, siga o processo de implantação configurado no servidor. Depois, confirme:

```text
https://56.125.167.156.sslip.io/actuator/health
https://56.125.167.156.sslip.io/login
```

O primeiro endereço deve responder `UP`; o segundo deve apresentar a tela de autenticação. Mudanças em OAuth exigem
que o URI autorizado no Google Cloud continue apontando para:

```text
https://56.125.167.156.sslip.io/login/oauth2/code/google
```

## Nota sobre os arquivos do repositório

O arquivo `compose.oracle.yaml` ainda tem esse nome por legado, mas contém a sobreposição de produção com Caddy,
HTTPS e banco PostgreSQL externo. Ele não significa que a aplicação esteja hospedada na Oracle. Uma eventual troca
de nome deve ser feita em uma alteração própria, coordenada com o processo já usado no servidor AWS.

## Publicação automática pelo GitHub

O arquivo `.github/workflows/deploy-production.yml` publica automaticamente na AWS a cada `push` para a branch
`main`. Ele executa os testes primeiro; se qualquer teste falhar, a produção não é alterada.

O workflow copia apenas o código versionado para `~/vagaradar-ai`, preserva o arquivo `.env` existente no servidor e
recria os containers. Ao final, ele aguarda o health check do container da aplicação ficar `healthy`.

### Configuração única

Crie uma chave SSH exclusiva para o GitHub Actions no seu computador. No PowerShell:

```powershell
ssh-keygen -t ed25519 -C "github-actions-vagaradar" -f "$env:USERPROFILE\.ssh\vagaradar_github_actions"
```

Não defina senha para essa chave: o GitHub Actions não consegue digitá-la. Guarde o arquivo privado com cuidado e
nunca o versione.

Copie o conteúdo do arquivo `vagaradar_github_actions.pub` e, no terminal SSH da AWS, execute substituindo o texto
entre aspas pela chave pública completa:

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
echo 'COLE_A_CHAVE_PUBLICA_AQUI' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

No repositório GitHub, acesse **Settings → Secrets and variables → Actions** e crie estes *Repository secrets*:

| Segredo | Valor |
| --- | --- |
| `AWS_HOST` | IP público atual da VM, por exemplo `56.125.167.156` |
| `AWS_SSH_PRIVATE_KEY` | Conteúdo completo do arquivo privado `vagaradar_github_actions` |
| `AWS_SSH_KNOWN_HOSTS` | Resultado de `ssh-keyscan -H 56.125.167.156` executado no seu computador |

Depois da configuração, `git push origin main` executará testes e, se tudo estiver correto, fará a publicação.
Em **Actions** no GitHub é possível acompanhar cada etapa e ler os logs. Se o IP público da Lightsail mudar, atualize
`AWS_HOST`, `AWS_SSH_KNOWN_HOSTS`, `CADDY_DOMAIN` e as configurações de domínio/OAuth relacionadas.
