const form = document.querySelector("#form");
const urlInput = document.querySelector("#api-url");
const tokenInput = document.querySelector("#api-token");
const status = document.querySelector("#status");

chrome.storage.local.get(["apiUrl", "apiToken"]).then(config => {
  urlInput.value = config.apiUrl || "";
  tokenInput.value = config.apiToken || "";
});

form.addEventListener("submit", async event => {
  event.preventDefault();
  const apiUrl = urlInput.value.replace(/\/$/, "");
  const origem = `${new URL(apiUrl).origin}/*`;
  const permitido = await chrome.permissions.request({ origins: [origem] });
  if (!permitido) { status.textContent = "Permissão para acessar o VagaRadar não foi concedida."; return; }
  await chrome.storage.local.set({ apiUrl, apiToken: tokenInput.value });
  status.textContent = "Configuração salva.";
});
