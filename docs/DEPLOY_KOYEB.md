# Deploy gratuito na Koyeb

Este guia publica o VagaRadar AI na Koyeb usando o repositório GitHub. O plano gratuito é suficiente para uso pessoal,
mas o serviço entra em repouso após uma hora sem acessos. Por isso, mantenha `GMAIL_SCHEDULER_ENABLED=false` nesse plano
e use a importação manual pelo painel.

## 1. Criar o banco PostgreSQL

1. Entre em [Koyeb](https://app.koyeb.com/) usando a conta GitHub.
2. Em **Create Database Service**, selecione **PostgreSQL** e a instância **Free**.
3. Copie os dados de conexão exibidos pela Koyeb: host, porta, database, username e password.

A variável `DATABASE_URL` do Spring deve ficar no formato JDBC:

```text
jdbc:postgresql://HOST:5432/NOME_DO_BANCO
```

## 2. Criar o serviço web

1. Em **Create Web Service**, escolha o repositório `Ric-ardo28/vagaradar-ai`, ramo `main`.
2. Selecione o método de build **Dockerfile** e mantenha `Dockerfile` na raiz.
3. Selecione a instância **Free**.
4. Em **Exposed ports**, configure a porta `8080`, pública, protocolo HTTP e caminho `/`.
5. Em health check, use HTTP em `/actuator/health`.

## 3. Adicionar variáveis e segredos

No serviço web, em **Environment variables and files**, adicione os valores abaixo. Marque segredos como secretos
na interface da Koyeb; nunca os coloque no GitHub.

```text
DATABASE_URL=jdbc:postgresql://HOST:5432/NOME_DO_BANCO
DATABASE_USERNAME=SEU_USUARIO_DO_BANCO
DATABASE_PASSWORD=SUA_SENHA_DO_BANCO
APP_ADMIN_USERNAME=SEU_USUARIO
APP_ADMIN_PASSWORD=SENHA_COM_12_OU_MAIS_CARACTERES
OPENAI_API_KEY=SUA_CHAVE
OPENAI_MODEL=gpt-5.6-terra
GOOGLE_CLIENT_ID=SEU_CLIENT_ID
GOOGLE_CLIENT_SECRET=SEU_CLIENT_SECRET
GMAIL_OAUTH_ENABLED=true
GMAIL_SCHEDULER_ENABLED=false
SESSION_COOKIE_SECURE=true
DISCORD_WEBHOOK_URL=OPCIONAL
DISCORD_MINIMUM_SCORE=70
```

Não é necessário configurar `PORT`: a Koyeb o fornece automaticamente e a aplicação já o respeita.

## 4. Ajustar OAuth do Google

Após a Koyeb concluir o deploy, copie o domínio público gerado, por exemplo `https://seu-servico.koyeb.app`.
No cliente OAuth do Google Cloud, acrescente este URI de redirecionamento autorizado:

```text
https://seu-servico.koyeb.app/login/oauth2/code/google
```

Mantenha também o URI local durante os testes. Depois, no endereço público, entre com o administrador e clique
em **Conectar Gmail** para autorizar novamente a conta no ambiente publicado.

## Verificação

Confirme estes dois endereços depois do deploy:

- `https://seu-servico.koyeb.app/actuator/health` deve retornar `UP`.
- `https://seu-servico.koyeb.app/login` deve abrir a tela de acesso.

O primeiro acesso depois de uma hora sem tráfego pode levar alguns segundos porque a instância gratuita desperta
automaticamente. As atualizações enviadas ao ramo `main` disparam novos deploys na Koyeb.
