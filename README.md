# VagaRadar AI

Backend em Java 21 e Spring Boot para cadastrar vagas de tecnologia, analisar compatibilidade com um perfil Java/Spring por IA e enviar alertas ao Discord.

O projeto também disponibiliza um painel web local em `http://localhost:8080/`. Ele lista as vagas importadas,
exibe o status de análise e permite iniciar a conexão Gmail ou a importação manual.

## Pré-requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 16+
- Uma chave da OpenAI para usar a análise por IA

## Configuração

Copie `.env.example` para `.env.local` e preencha os valores necessários. O `.env.local` é carregado localmente e não é versionado.

Além das integrações, defina `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD`. A senha deve ter pelo menos 12 caracteres,
ficar somente no ambiente local ou no gerenciador de segredos da plataforma e nunca ser incluída no Git.

## Executar localmente

```bash
mvn spring-boot:run
```

As migrações do banco são executadas automaticamente pelo Flyway.

Depois de iniciar, abra `http://localhost:8080/` para acessar o painel. A ação **Importar e analisar** pode consumir
créditos da OpenAI; por isso, ela sempre exige uma confirmação no navegador.

## Acesso e segurança

O painel e todas as rotas de negócio exigem login em `http://localhost:8080/login`. A aplicação protege ações de
escrita com CSRF e mantém a sessão por 8 horas, por padrão. O endpoint `GET /actuator/health` é a única rota pública,
para uso por verificações de saúde da infraestrutura.

Após entrar no painel, use **Conectar Gmail** para iniciar o OAuth. O navegador retorna ao painel depois do consentimento.

## Perfil profissional

O projeto mantém um perfil-base fixo no código, voltado a Java, Spring Boot e desenvolvimento backend júnior.
No painel, use **Meu perfil** para criar uma versão personalizada. Essa versão fica no PostgreSQL e será usada nas
próximas análises. A opção **Restaurar padrão** remove somente a personalização e volta imediatamente ao perfil-base
do código.

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| POST | `/api/vagas` | Cadastra uma vaga |
| GET | `/api/vagas` | Lista vagas |
| GET | `/api/vagas/{id}` | Busca uma vaga |
| POST | `/api/vagas/{id}/analise` | Gera e persiste a análise de compatibilidade |
| POST | `/api/vagas/{id}/descartar` | Move uma vaga para o histórico de descartadas |
| GET | `/api/perfil` | Retorna o perfil-base ou a versão personalizada ativa |
| PUT | `/api/perfil` | Salva a versão personalizada do perfil |
| DELETE | `/api/perfil` | Remove a personalização e restaura o perfil-base |

Exemplo de cadastro:

```json
{
  "linkedinId": "123456",
  "cargo": "Desenvolvedor Java Júnior",
  "empresa": "Empresa Exemplo",
  "descricao": "Atuação com Java, Spring Boot, APIs REST e PostgreSQL.",
  "localizacao": "São Paulo",
  "modeloTrabalho": "HIBRIDO",
  "link": "https://www.linkedin.com/jobs/view/123456",
  "dataPublicacao": "2026-08-07T12:00:00Z"
}
```

## Docker

Com as variáveis configuradas, execute:

```bash
docker compose up --build
```

Para executar o backend pelo Maven e o banco pelo Docker, inicie somente o PostgreSQL com
`docker compose up -d postgres`. Ele fica disponível em `localhost:5433`, evitando conflito
com uma instalação local do PostgreSQL que use a porta padrão `5432`.

O PostgreSQL ficará disponível no serviço `postgres`; a aplicação aguarda a verificação de saúde do banco antes de iniciar.

Para produção, aplique também a composição de produção. Ela remove a exposição da porta do PostgreSQL e marca o cookie
de sessão como seguro; publique a aplicação atrás de HTTPS.

```bash
docker compose -f compose.yaml -f compose.production.yaml up -d --build
```

## Health check e deploy

O health check público fica em `GET /actuator/health`. Ele é usado pelo Docker e pode ser configurado pela
plataforma de hospedagem para confirmar que a aplicação e o banco estão disponíveis.

Antes de publicar, configure na plataforma as mesmas variáveis de `.env.example`, sem versionar valores reais:

- `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` de um PostgreSQL gerenciado;
- `OPENAI_API_KEY` e, opcionalmente, `DISCORD_WEBHOOK_URL`;
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` e `GMAIL_OAUTH_ENABLED=true` se for usar Gmail;
- `GMAIL_SCHEDULER_ENABLED=false` inicialmente. Ative-o somente após validar custos e permissões.
- `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` em um gerenciador de segredos; use `SESSION_COOKIE_SECURE=true` sob HTTPS.

Em produção, inclua a URL pública no URI de redirecionamento do cliente OAuth do Google. Exemplo:
`https://seu-dominio.com/login/oauth2/code/google`.

Os testes de integração usam Testcontainers com PostgreSQL 16 quando o Docker Engine está disponível. Em ambientes
sem Docker, eles são ignorados; os testes unitários continuam sendo executados normalmente.

O serviço PostgreSQL não deve ter porta exposta publicamente em produção. Mantenha-o acessível apenas pela rede interna
dos containers e restrinja o acesso administrativo ao banco, pois ele contém os tokens OAuth persistidos.

## Integrações

- OpenAI: obrigatória apenas para gerar análises; usa `OPENAI_API_KEY`.
- Discord: opcional; configure `DISCORD_WEBHOOK_URL` para receber alertas.
- Gmail: leitura de alertas via OAuth 2.0 do Google; exige `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` locais.

## Conectar Gmail

Com `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` configurados, abra
`http://localhost:8080/oauth2/authorization/google`. Após aprovar o consentimento, use a mesma sessão do
navegador para:

- `GET /api/gmail/alerts`: listar alertas candidatos em modo somente leitura;
- `POST /api/gmail/import`: ler o conteúdo dos alertas, extrair links de vagas do LinkedIn e persistir apenas
  as vagas ainda não cadastradas.
- `POST /api/gmail/process`: importar vagas novas, analisá-las com a OpenAI e enviar ao Discord apenas as que
  alcançarem `DISCORD_MINIMUM_SCORE` (70 por padrão).

## Automação agendada

Depois da primeira autorização, o cliente OAuth do Google é persistido no PostgreSQL para que o backend possa
renovar o acesso ao Gmail. Para habilitar a execução automática, configure localmente:

```properties
GMAIL_SCHEDULER_ENABLED=true
GMAIL_SCHEDULER_FIXED_DELAY=PT6H
GMAIL_SCHEDULER_INITIAL_DELAY=PT5M
```

O ciclo fica desativado por padrão, pois pode consumir créditos da OpenAI. Os tokens OAuth são dados sensíveis:
não os versionar, não os registrar em logs e usar um banco de dados protegido em ambientes de produção.
