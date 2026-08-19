# VagaRadar AI — Contexto do Projeto

## Objetivo

O VagaRadar AI é uma aplicação web que recebe oportunidades de tecnologia, analisa cada vaga com IA e informa o quanto ela combina com o perfil profissional do usuário, em uma pontuação de 0 a 100.

O foco inicial são vagas de Desenvolvedor Java, Backend, Spring Boot, estágio, trainee, desenvolvedor júnior, Engenharia de Software e áreas relacionadas.

O perfil-base para a análise inclui Java, Spring Boot, APIs REST, JPA/Hibernate, PostgreSQL, SQL e Git; há conhecimento básico de Docker, HTML, CSS e JavaScript. O objetivo profissional é estágio ou desenvolvimento backend júnior, com preferência por São Paulo, híbrido ou remoto.

## Estado atual

As integrações com Gmail, OpenAI e Discord, além da prevenção de duplicidade, estão concluídas. O backend lê alertas
do Gmail, importa vagas inéditas, analisa todas as vagas pendentes com OpenAI e registra na fila do Discord as
compatibilidades acima do limite configurado. Um processamento independente envia essa fila, com novas tentativas em
caso de falha temporária. Uma vaga pendente também pode ser analisada individualmente pelo painel. O cliente OAuth
do Google é persistido no PostgreSQL para permitir renovação de token e automação sem um navegador aberto.

O agendamento está implementado e permanece desativado por padrão através de `GMAIL_SCHEDULER_ENABLED=false`; na AWS,
ele está habilitado com intervalo de uma hora. Há testes para fluxos HTTP, importação, processamento e arquivos Docker
para executar a aplicação com PostgreSQL. O painel requer autenticação de administrador por variáveis de ambiente;
ações de escrita usam CSRF. Ele exibe a conta Gmail conectada, o estado do Discord, filtros de vagas, a data/hora em que
cada vaga foi analisada e uma interface responsiva para desktop e celular.
O perfil-base continua fixo no código para preservar o direcionamento pessoal do projeto. O painel permite salvar uma
sobreposição personalizada no PostgreSQL ou removê-la para restaurar o padrão; cada nova análise utiliza a versão ativa.

## Situação de publicação

O VagaRadar AI está publicado na AWS, em uma instância Amazon Lightsail chamada `vagaradar-app`, na região
São Paulo (`sa-east-1a`). A instância está em execução e oferece 2 vCPUs, 4 GB de RAM e 80 GB SSD. O acesso público
é feito por HTTPS em `https://56.125.167.156.sslip.io/login`. Em 18 de agosto de 2026, o health check público
`/actuator/health` respondeu `UP`.

A borda pública permite HTTP (80), HTTPS (443) e SSH (22); não há balanceador, CDN ou snapshots automáticos
configurados. O projeto mantém composições Docker e um guia operacional para esse ambiente; o guia da Oracle Cloud
permanece somente como referência histórica e não representa a infraestrutura ativa.

As melhorias que dependem de um servidor público com HTTPS estão registradas em
[IMPLEMENTACOES_FUTURAS.md](IMPLEMENTACOES_FUTURAS.md).

No Google Cloud, o OAuth está em modo de teste: apenas contas incluídas como usuários de teste podem conectar o Gmail.
Para uso aberto a qualquer conta Google, será necessária a publicação e verificação do aplicativo OAuth.

## Regras de trabalho

- Implementar somente a fase solicitada; não avançar automaticamente.
- Ao concluir uma fase, informar arquivos alterados, fluxo implementado, validações executadas e uma sugestão de commit; então aguardar a próxima instrução.
- Manter o código organizado por responsabilidades. A estrutura prevista inclui `controller`, `service`, `repository`, `entity`, `dto`, `config`, `integration`, `exception` e, quando necessário, `scheduler`.
- Criar DTOs para contratos de entrada e saída da API; não expor entidades JPA diretamente.
- Validar entradas com Bean Validation e adotar tratamento global de erros quando a fase correspondente for iniciada.
- Escrever testes proporcionais a cada funcionalidade e manter a compilação verde.
- Usar Git com commits pequenos, claros e focados.

## Segurança e configuração

- Segredos e configurações específicas de ambiente devem ser fornecidos por variáveis de ambiente ou mecanismos equivalentes; nunca devem ser versionados.
- O projeto já espera `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` para execução com PostgreSQL.
- Futuras chaves, como `OPENAI_API_KEY`, webhook do Discord e credenciais do Gmail, também deverão vir do ambiente.
- Não registrar tokens, senhas, conteúdo sensível de e-mails ou dados pessoais em logs. Tokens OAuth persistidos
  no banco exigem acesso restrito ao banco e criptografia de disco ou de dados em ambientes de produção.
- A senha do administrador vem de `APP_ADMIN_PASSWORD`, com no mínimo 12 caracteres. Em produção, usar HTTPS e
  `SESSION_COOKIE_SECURE=true`; o banco não deve expor uma porta pública.

## Integrações implementadas

- **OpenAI:** analisa a vaga contra o perfil profissional e produz pontuação, pontos fortes, lacunas e recomendação.
- **Discord:** publica alertas das vagas que atingem a nota mínima configurada, com nova tentativa em caso de limite temporário.
- **Gmail:** lê alertas do LinkedIn via OAuth, extrai vagas por link e mostra o e-mail da conta conectada no painel.
