function textoDoPrimeiro(seletores) {
  for (const seletor of seletores) {
    const elemento = document.querySelector(seletor);
    if (elemento?.innerText?.trim()) return elemento.innerText.trim();
  }
  return "";
}

function extrairDoCartaoSelecionado() {
  const marcador = Array.from(document.querySelectorAll("span")).find(elemento =>
    /^(Selecionada|Selected), /.test(elemento.textContent?.trim() || "")
  );
  const bloco = marcador?.closest("p")?.parentElement?.parentElement;
  if (!bloco) return {};

  const linhas = bloco.innerText.split("\n").map(linha => linha.trim()).filter(Boolean);
  const indiceMarcador = linhas.findIndex(linha => /^(Selecionada|Selected), /.test(linha));
  if (indiceMarcador < 0) return {};

  return {
    cargo: linhas[indiceMarcador + 1] || "",
    empresa: linhas[indiceMarcador + 2] || "",
    localizacao: linhas[indiceMarcador + 3] || ""
  };
}

function extrairVaga() {
  const url = new URL(window.location.href);
  const linkedinId = url.pathname.match(/\/jobs\/view\/(\d+)/)?.[1] || url.searchParams.get("currentJobId");
  const link = linkedinId ? `https://www.linkedin.com/jobs/view/${linkedinId}` : url.origin + url.pathname;
  const cartaoSelecionado = extrairDoCartaoSelecionado();
  return {
    linkedinId,
    cargo: textoDoPrimeiro([
      "[data-test-id='job-title']", "[data-test-job-title]", ".job-details-jobs-unified-top-card__job-title",
      ".jobs-unified-top-card__job-title", "main h1", "h1"
    ]) || cartaoSelecionado.cargo,
    empresa: textoDoPrimeiro([
      "[data-test-id='job-company-name']", "[data-test-job-company-name]",
      ".job-details-jobs-unified-top-card__company-name a", ".job-details-jobs-unified-top-card__company-name",
      ".jobs-unified-top-card__company-name a", ".jobs-unified-top-card__company-name"
    ]) || cartaoSelecionado.empresa,
    descricao: textoDoPrimeiro([
      "[data-test-id='job-details']", ".jobs-description__content", ".jobs-description-content__text", "#job-details"
    ]) || document.body.innerText.slice(0, 8000),
    localizacao: textoDoPrimeiro([
      "[data-test-id='job-location']", ".job-details-jobs-unified-top-card__primary-description-container",
      ".jobs-unified-top-card__primary-description-container"
    ]) || cartaoSelecionado.localizacao,
    modeloTrabalho: "NAO_INFORMADO",
    link,
    dataPublicacao: null
  };
}

function esperar(milisegundos) {
  return new Promise(resolve => setTimeout(resolve, milisegundos));
}

async function extrairVagaQuandoDisponivel() {
  for (let tentativa = 0; tentativa < 10; tentativa++) {
    const vaga = extrairVaga();
    if (vaga.cargo && vaga.empresa) return vaga;
    await esperar(500);
  }
  return extrairVaga();
}

chrome.runtime.onMessage.addListener((mensagem, _, responder) => {
  if (mensagem.tipo !== "EXTRAIR_VAGA") return;
  extrairVagaQuandoDisponivel().then(responder);
  return true;
});
