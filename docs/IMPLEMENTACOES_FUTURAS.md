# Implementações futuras

Este documento reúne ideias aprovadas para o VagaRadar AI que ainda não serão feitas agora. Cada item deve indicar seu objetivo, dependências e o que falta decidir antes de começar.

## Integração implementada — ativação depende do ambiente

### Processar alertas do LinkedIn assim que chegarem

**Objetivo:** analisar e colocar na fila do Discord as vagas novas logo após a chegada do alerta no Gmail, sem esperar
a rotina programada de uma hora.

**Fluxo implementado:**

1. O Gmail avisa o Google Cloud Pub/Sub que a caixa de entrada foi alterada.
2. O Pub/Sub chama um endereço seguro do VagaRadar AI.
3. A aplicação consulta somente as mensagens novas, importa as vagas, faz a análise e registra os alertas elegíveis
   na fila do Discord.

**Dependências:**

- Aplicação publicada em uma VM ou servidor com endereço HTTPS público (requisito já atendido pela AWS).
- Projeto no Google Cloud com Pub/Sub habilitado.
- Renovação periódica da assinatura de monitoramento do Gmail.

**Situação:** o código já contém o endpoint `/api/gmail/push`, a validação do JWT OIDC e a renovação periódica
do monitoramento Gmail. A ativação exige configurar tópico, assinatura autenticada, permissões e variáveis do
ambiente conforme o [README](../README.md#processamento-imediato-por-e-mail-gmail-push). A presença do código
não confirma que a integração esteja habilitada no servidor. O agendamento continua como alternativa.

## Próximas ideias a avaliar

### Corrigir títulos das vagas já importadas

Os novos alertas já usam a extração corrigida de título. As vagas antigas podem ser revisadas por uma reimportação controlada dos alertas, sem duplicar os links existentes.

**Situação:** aguarda decisão sobre a reimportação.

## Como registrar uma nova ideia

Para cada implementação futura, adicionar uma seção com:

- objetivo para o usuário;
- dependências ou custos;
- riscos ou decisões pendentes;
- situação atual.
