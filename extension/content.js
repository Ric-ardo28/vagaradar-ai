function textoDoPrimeiro(seletores) {
  for (const seletor of seletores) {
    const elemento = document.querySelector(seletor);
    if (elemento?.innerText?.trim()) return elemento.innerText.trim();
  }
  return "";
}

function extrairVaga() {
  const link = window.location.href.split("?")[0];
  const linkedinId = link.match(/\/jobs\/view\/(\d+)/)?.[1] || null;
  return {
    linkedinId,
    cargo: textoDoPrimeiro(["h1", ".job-details-jobs-unified-top-card__job-title"]),
    empresa: textoDoPrimeiro([".job-details-jobs-unified-top-card__company-name a", ".job-details-jobs-unified-top-card__company-name"]),
    descricao: textoDoPrimeiro([".jobs-description__content", "#job-details"]) || document.body.innerText.slice(0, 8000),
    localizacao: textoDoPrimeiro([".job-details-jobs-unified-top-card__primary-description-container"]),
    modeloTrabalho: "NAO_INFORMADO",
    link,
    dataPublicacao: null
  };
}

chrome.runtime.onMessage.addListener((mensagem, _, responder) => {
  if (mensagem.tipo === "EXTRAIR_VAGA") responder(extrairVaga());
});
