# VagaRadar AI

Backend em Java 21 e Spring Boot para cadastrar vagas de tecnologia, analisar compatibilidade com um perfil Java/Spring por IA e enviar alertas ao Discord.

## Pré-requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 16+
- Uma chave da OpenAI para usar a análise por IA

## Configuração

Copie `.env.example` para `.env.local` e preencha os valores necessários. O `.env.local` é carregado localmente e não é versionado.

## Executar localmente

```bash
mvn spring-boot:run
```

As migrações do banco são executadas automaticamente pelo Flyway.

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| POST | `/api/vagas` | Cadastra uma vaga |
| GET | `/api/vagas` | Lista vagas |
| GET | `/api/vagas/{id}` | Busca uma vaga |
| POST | `/api/vagas/{id}/analise` | Gera e persiste a análise de compatibilidade |

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
