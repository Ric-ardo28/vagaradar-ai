# VagaRadar AI — Contexto do Projeto

## Objetivo

O VagaRadar AI será um backend que recebe oportunidades de tecnologia, analisa cada vaga com IA e informa o quanto ela combina com o perfil profissional do usuário, em uma pontuação de 0 a 100.

O foco inicial são vagas de Desenvolvedor Java, Backend, Spring Boot, estágio, trainee, desenvolvedor júnior, Engenharia de Software e áreas relacionadas.

O perfil-base para a análise inclui Java, Spring Boot, APIs REST, JPA/Hibernate, PostgreSQL, SQL e Git; há conhecimento básico de Docker, HTML, CSS e JavaScript. O objetivo profissional é estágio ou desenvolvimento backend júnior, com preferência por São Paulo, híbrido ou remoto.

## Estado atual

As fases de Gmail, OpenAI, Discord e prevenção de duplicidade estão concluídas. O backend lê alertas do Gmail,
importa vagas inéditas, analisa somente as novas com OpenAI e envia ao Discord as compatibilidades acima do limite
configurado. O cliente OAuth do Google é persistido no PostgreSQL para permitir renovação de token e automação sem
um navegador aberto.

O agendamento está implementado, porém permanece desativado por padrão através de `GMAIL_SCHEDULER_ENABLED=false`.
Há testes para fluxos HTTP, importação, processamento e arquivos Docker para executar a aplicação com PostgreSQL.
O painel agora requer autenticação de administrador por variáveis de ambiente; ações de escrita usam CSRF.
O perfil-base continua fixo no código para preservar o direcionamento pessoal do projeto. O painel permite salvar uma
sobreposição personalizada no PostgreSQL ou removê-la para restaurar o padrão; cada nova análise utiliza a versão ativa.

## Roadmap incremental

1. Inicialização do projeto e contexto mestre.
2. Modelagem inicial de vagas e análise, com persistência PostgreSQL.
3. DTOs, validação e API REST inicial.
4. Tratamento de erros e respostas padronizadas.
5. Integração futura com OpenAI para análise e pontuação.
6. Integração futura com Discord para alertas.
7. Integração futura com Gmail ou outra fonte de alertas de vaga.
8. Prevenção de duplicidade, automação agendada e refinamentos.
9. Testes de integração, documentação e preparação para deploy. Em andamento: health check e Testcontainers foram adicionados;
   falta apenas escolher uma plataforma antes de uma publicação real.
10. Segurança de acesso pessoal: login de administrador, proteção CSRF, sessão e cabeçalhos de segurança. Concluída.
11. Perfil profissional editável: perfil-base fixo no código, personalização opcional persistida e restauração do padrão. Concluída.
12. Preparação para deploy gratuito: compatibilidade com porta da plataforma e guia de publicação na Koyeb. Concluída.

As fases poderão ser ajustadas conforme as decisões do desenvolvimento.

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

## Integrações futuras

- **OpenAI:** analisar a descrição da vaga contra o perfil profissional e produzir pontuação, pontos fortes, lacunas e recomendação.
- **Discord:** publicar alertas de vagas com melhor compatibilidade.
- **Gmail:** receber ou ler alertas de vagas como uma possível fonte de entrada.

Essas integrações não fazem parte da Fase 1 e só serão adicionadas quando solicitadas.
