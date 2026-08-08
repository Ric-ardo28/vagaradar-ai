const elements = {
  vacancies: document.querySelector('#vacancies'), status: document.querySelector('#status-message'),
  total: document.querySelector('#total-count'), analyzed: document.querySelector('#analyzed-count'),
  waiting: document.querySelector('#new-count'), refresh: document.querySelector('#refresh-button'),
  gmail: document.querySelector('#gmail-button'), process: document.querySelector('#process-button'),
  dialog: document.querySelector('#analysis-dialog'), analysis: document.querySelector('#analysis-content')
};

const escapeHtml = (value = '') => String(value).replace(/[&<>'"]/g, char => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', "'":'&#039;', '"':'&quot;' }[char]));
const statusLabel = status => ({ RECEBIDA:'Aguardando análise', ANALISADA:'Analisada', DESCARTADA:'Descartada' }[status] ?? status);
const formatWorkModel = model => ({ REMOTO:'Remoto', HIBRIDO:'Híbrido', PRESENCIAL:'Presencial', NAO_INFORMADO:'Modelo não informado' }[model] ?? 'Modelo não informado');

async function request(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { Accept:'application/json', ...(options.headers || {}) } });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `Não foi possível concluir a operação (${response.status}).`);
  }
  return response.json();
}

function updateStats(vacancies) {
  const analyzed = vacancies.filter(vacancy => vacancy.status === 'ANALISADA').length;
  elements.total.textContent = vacancies.length;
  elements.analyzed.textContent = analyzed;
  elements.waiting.textContent = vacancies.filter(vacancy => vacancy.status === 'RECEBIDA').length;
}

function renderVacancies(vacancies) {
  updateStats(vacancies);
  if (!vacancies.length) {
    elements.vacancies.replaceChildren(document.querySelector('#empty-state-template').content.cloneNode(true));
    return;
  }
  elements.vacancies.innerHTML = vacancies.map(vacancy => `
    <article class="vacancy">
      <div>
        <h3>${escapeHtml(vacancy.cargo)}</h3>
        <p class="company">${escapeHtml(vacancy.empresa)}</p>
        <p class="details">${escapeHtml(vacancy.localizacao || 'Localização não informada')} · ${formatWorkModel(vacancy.modeloTrabalho)}</p>
      </div>
      <div class="vacancy-actions">
        <span class="badge badge-${vacancy.status.toLowerCase()}">${statusLabel(vacancy.status)}</span>
        <button class="analyze-button" data-vacancy-id="${vacancy.id}" type="button">${vacancy.status === 'ANALISADA' ? 'Ver análise' : 'Analisar'}</button>
      </div>
    </article>`).join('');
}

async function loadVacancies() {
  elements.status.textContent = 'Atualizando...';
  try {
    const vacancies = await request('/api/vagas');
    renderVacancies(vacancies);
    elements.status.textContent = `${vacancies.length} vaga${vacancies.length === 1 ? '' : 's'} carregada${vacancies.length === 1 ? '' : 's'}.`;
  } catch (error) {
    elements.status.textContent = error.message;
    elements.status.classList.add('error');
  }
}

function showAnalysis(analysis) {
  elements.analysis.innerHTML = `
    <p class="eyebrow">ANÁLISE DE COMPATIBILIDADE</p>
    <h2>${escapeHtml(analysis.nivelCompatibilidade)}</h2>
    <p class="score"><strong>${analysis.pontuacao}</strong><span>/ 100</span></p>
    <section class="analysis-block"><h3>Pontos fortes</h3><p>${escapeHtml(analysis.pontosFortes || 'Não informado')}</p></section>
    <section class="analysis-block"><h3>Pontos a desenvolver</h3><p>${escapeHtml(analysis.pontosFaltantes || 'Não informado')}</p></section>
    <section class="analysis-block"><h3>Recomendação</h3><p>${escapeHtml(analysis.recomendacao || 'Não informado')}</p></section>`;
  elements.dialog.showModal();
}

elements.vacancies.addEventListener('click', async event => {
  const button = event.target.closest('[data-vacancy-id]');
  if (!button) return;
  button.disabled = true;
  button.textContent = 'Carregando...';
  try {
    showAnalysis(await request(`/api/vagas/${button.dataset.vacancyId}/analise`, { method:'POST' }));
    await loadVacancies();
  } catch (error) { elements.status.textContent = error.message; elements.status.classList.add('error'); }
  finally { button.disabled = false; }
});

elements.gmail.addEventListener('click', async () => {
  try { window.location.assign((await request('/api/gmail/connect')).authorizationUrl); }
  catch (error) { elements.status.textContent = error.message; elements.status.classList.add('error'); }
});

elements.process.addEventListener('click', async () => {
  if (!window.confirm('Importar novas vagas e analisá-las? Esta ação pode consumir créditos da OpenAI.')) return;
  elements.process.disabled = true; elements.process.textContent = 'Processando...';
  try {
    const result = await request('/api/gmail/process', { method:'POST' });
    elements.status.textContent = `${result.importacao.vagasImportadas} vaga(s) importada(s) e ${result.vagasAnalisadas} analisada(s).`;
    await loadVacancies();
  } catch (error) {
    elements.status.textContent = error.message.includes('403') ? 'Conecte o Gmail antes de importar.' : error.message;
    elements.status.classList.add('error');
  } finally { elements.process.disabled = false; elements.process.textContent = 'Importar e analisar'; }
});

elements.refresh.addEventListener('click', loadVacancies);
document.querySelector('#dialog-close').addEventListener('click', () => elements.dialog.close());
loadVacancies();
