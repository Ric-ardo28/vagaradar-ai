# VagaRadar AI — Contexto do Projeto

## Objetivo

O VagaRadar AI será um backend que recebe oportunidades de tecnologia, analisa cada vaga com IA e informa o quanto ela combina com o perfil profissional do usuário, em uma pontuação de 0 a 100.

O foco inicial são vagas de Desenvolvedor Java, Backend, Spring Boot, estágio, trainee, desenvolvedor júnior, Engenharia de Software e áreas relacionadas.

O perfil-base para a análise inclui Java, Spring Boot, APIs REST, JPA/Hibernate, PostgreSQL, SQL e Git; há conhecimento básico de Docker, HTML, CSS e JavaScript. O objetivo profissional é estágio ou desenvolvimento backend júnior, com preferência por São Paulo, híbrido ou remoto.

## Estado atual

**Fase 8 — prevenção de duplicidade** está concluída: vagas com o mesmo link ou identificador LinkedIn não podem ser cadastradas novamente. A proteção acontece no serviço e no banco de dados.

**Fase 7 — Gmail** permanece pendente: para leitura automática pelo backend serão necessárias credenciais OAuth próprias do Google. O conector de Gmail do Codex não substitui essa autorização da aplicação.

**Fase 9 — qualidade e preparação para deploy** está concluída: há testes para os fluxos HTTP principais, documentação de uso, exemplo seguro de variáveis e arquivos Docker para executar a aplicação com PostgreSQL.

## Roadmap incremental

1. Inicialização do projeto e contexto mestre.
2. Modelagem inicial de vagas e análise, com persistência PostgreSQL.
3. DTOs, validação e API REST inicial.
4. Tratamento de erros e respostas padronizadas.
5. Integração futura com OpenAI para análise e pontuação.
6. Integração futura com Discord para alertas.
7. Integração futura com Gmail ou outra fonte de alertas de vaga.
8. Prevenção de duplicidade, agendamento e refinamentos.
9. Testes de integração, documentação e preparação para deploy.

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
- Não registrar tokens, senhas, conteúdo sensível de e-mails ou dados pessoais em logs.

## Integrações futuras

- **OpenAI:** analisar a descrição da vaga contra o perfil profissional e produzir pontuação, pontos fortes, lacunas e recomendação.
- **Discord:** publicar alertas de vagas com melhor compatibilidade.
- **Gmail:** receber ou ler alertas de vagas como uma possível fonte de entrada.

Essas integrações não fazem parte da Fase 1 e só serão adicionadas quando solicitadas.
