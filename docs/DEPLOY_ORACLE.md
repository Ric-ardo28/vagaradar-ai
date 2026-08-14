# Referência histórica: deploy na Oracle Cloud Always Free

> A Oracle Cloud não é o ambiente ativo do VagaRadar AI. A implantação atual está na AWS; consulte
> [DEPLOY_AWS.md](DEPLOY_AWS.md) para o procedimento operacional atual. Este arquivo foi preservado somente para
> registrar a alternativa de infraestrutura anteriormente avaliada.

Este guia publica o VagaRadar AI em uma VM Always Free da Oracle Cloud e usa o Supabase como PostgreSQL gerenciado.
Assim, a VM executa somente a aplicacao e o Caddy; nenhum banco fica exposto na internet.

## Recursos usados

- VM `VM.Standard.A1.Flex`: 1 OCPU e 6 GB de RAM (Always Free), quando houver capacidade;
- alternativa para uso pessoal/testes: `VM.Standard.E2.1.Micro` com 1 GB de RAM;
- projeto Supabase com banco PostgreSQL saudavel;
- VCN `vagaradar-vcn` e subnet publica `vagaradar-public-subnet`;
- portas publicas 80 e 443 para o site; 22 e reservado para administracao SSH;
- Docker Compose com volumes persistentes para PostgreSQL e Caddy.

> A disponibilidade de VMs Ampere pode faltar temporariamente em uma zona. Se isso acontecer, tente outra zona
> de disponibilidade ou tente novamente mais tarde. Nao substitua a VM por uma forma paga sem uma decisao explicita.

## 1. Criar a VM

No console da Oracle, crie uma instancia com estes valores:

```text
Nome: vagaradar-server
Imagem: Oracle Linux 9
Forma: VM.Standard.A1.Flex (1 OCPU, 6 GB)
Rede: vagaradar-vcn / vagaradar-public-subnet
IPv4 publico: habilitado
Chave SSH: gerar e baixar a chave privada
```

Guarde a chave privada baixada em local seguro. Ela nao deve entrar no repositorio nem ser compartilhada.

Se a forma A1 nao tiver capacidade, use `VM.Standard.E2.1.Micro`. Ela e suficiente para o uso pessoal inicial porque
o banco ficara no Supabase; mantenha o limite de memoria ja definido na composicao Oracle.

## 2. Preparar o servidor

Conecte-se usando a chave privada baixada e o IP publico exibido pela Oracle:

```bash
ssh -i /caminho/para/sua-chave.key opc@IP_PUBLICO
```

Na VM, instale e habilite o Docker:

```bash
sudo dnf update -y
sudo dnf install -y dnf-plugins-core git
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker opc
exit
```

Entre novamente por SSH para que o grupo `docker` seja aplicado.

## 3. Copiar o projeto e configurar segredos

```bash
git clone https://github.com/Ric-ardo28/vagaradar-ai.git
cd vagaradar-ai
cp .env.example .env
chmod 600 .env
```

Edite `.env` no servidor e preencha todos os valores reais. Nunca envie esse arquivo ao GitHub. No painel do Supabase,
abra **Connect** e copie os dados de conexao do banco (de preferencia o *pooler*). Converta a URL para JDBC e mantenha
`sslmode=require`. Inclua:

```text
DATABASE_URL=jdbc:postgresql://HOST:PORT/postgres?sslmode=require
DATABASE_USERNAME=postgres.PROJECT_REF
DATABASE_PASSWORD=senha-do-banco-do-supabase
APP_ADMIN_USERNAME=ricardo
APP_ADMIN_PASSWORD=uma-senha-com-12-ou-mais-caracteres
OPENAI_API_KEY=sua-chave
GOOGLE_CLIENT_ID=seu-client-id
GOOGLE_CLIENT_SECRET=seu-client-secret
GMAIL_OAUTH_ENABLED=true
GMAIL_SCHEDULER_ENABLED=false
SESSION_COOKIE_SECURE=true
CADDY_DOMAIN=IP_PUBLICO.sslip.io
```

Substitua `IP_PUBLICO` pelo IP da VM. O dominio `sslip.io` resolve automaticamente para esse IP, permitindo que
o Caddy obtenha um certificado HTTPS. Antes de iniciar, confirme que as portas 80 e 443 estao liberadas na VCN.

## 4. Iniciar a aplicacao

```bash
docker compose -f compose.yaml -f compose.oracle.yaml up -d --build
docker compose -f compose.yaml -f compose.oracle.yaml ps
```

A composicao Oracle nao inicia o container `postgres` local: a aplicacao se conecta ao Supabase por conexao
criptografada, preservando a memoria da VM.

Confira os logs se algum container nao ficar saudavel:

```bash
docker compose -f compose.yaml -f compose.oracle.yaml logs -f app caddy
```

## 5. Configurar OAuth do Google para producao

No cliente OAuth do Google Cloud, acrescente este URI de redirecionamento autorizado:

```text
https://IP_PUBLICO.sslip.io/login/oauth2/code/google
```

Depois, acesse `https://IP_PUBLICO.sslip.io/login`, entre com o administrador e autorize novamente o Gmail.
Somente depois do primeiro teste bem-sucedido, altere `GMAIL_SCHEDULER_ENABLED=true` e reinicie o stack.

## Atualizacoes e verificacao

Para atualizar o sistema na VM:

```bash
git pull --ff-only origin main
docker compose -f compose.yaml -f compose.oracle.yaml up -d --build
```

Verifique:

```text
https://IP_PUBLICO.sslip.io/actuator/health
https://IP_PUBLICO.sslip.io/login
```

Restrinja a regra SSH da VCN ao seu IP publico assim que tiver uma forma segura de acessar a VM.
