const vagaElement = document.querySelector("#vaga");
const statusElement = document.querySelector("#status");
const salvarButton = document.querySelector("#salvar");
let vagaAtual;

async function carregarVaga() {
  const [aba] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!aba?.url || !/^https:\/\/www\.linkedin\.com\/jobs\/view\/\d+/.test(aba.url)) {
    throw new Error("Abra a página individual da vaga no LinkedIn para usar a extensão.");
  }
  vagaAtual = await chrome.tabs.sendMessage(aba.id, { tipo: "EXTRAIR_VAGA" });
  if (!vagaAtual.cargo || !vagaAtual.empresa) throw new Error("Não foi possível identificar o cargo ou a empresa nesta página.");
  vagaElement.textContent = `${vagaAtual.cargo} · ${vagaAtual.empresa}`;
  salvarButton.disabled = false;
}

salvarButton.addEventListener("click", async () => {
  salvarButton.disabled = true;
  statusElement.textContent = "Salvando e analisando...";
  const resultado = await chrome.runtime.sendMessage({ tipo: "SALVAR_E_ANALISAR", vaga: vagaAtual });
  statusElement.textContent = resultado.erro
    ? resultado.erro
    : `${resultado.cargo}: compatibilidade ${resultado.pontuacao}/100.`;
});

document.querySelector("#configurar").addEventListener("click", () => chrome.runtime.openOptionsPage());
carregarVaga().catch(error => { vagaElement.textContent = error.message; });
