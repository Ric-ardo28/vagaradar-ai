chrome.runtime.onMessage.addListener((mensagem, _, responder) => {
  if (mensagem.tipo !== "SALVAR_E_ANALISAR") return;

  salvarEAnalisar(mensagem.vaga)
    .then(responder)
    .catch(error => responder({ erro: error.message }));
  return true;
});

async function salvarEAnalisar(vaga) {
  const { apiUrl, apiToken } = await chrome.storage.local.get(["apiUrl", "apiToken"]);
  if (!apiUrl || !apiToken) throw new Error("Configure a URL e o token do VagaRadar nas opções da extensão.");

  const response = await fetch(`${apiUrl.replace(/\/$/, "")}/api/extensao/vagas`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Authorization": `Bearer ${apiToken}` },
    body: JSON.stringify(vaga)
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message || `Não foi possível salvar a vaga (${response.status}).`);
  return { pontuacao: body.analise.pontuacao, cargo: body.vaga.cargo };
}
