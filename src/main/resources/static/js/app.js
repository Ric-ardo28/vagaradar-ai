const elements = {
  vacancies: document.querySelector('#vacancies'), status: document.querySelector('#status-message'),
  total: document.querySelector('#total-count'), analyzed: document.querySelector('#analyzed-count'),
  waiting: document.querySelector('#new-count'), refresh: document.querySelector('#refresh-button'),
  gmail: document.querySelector('#gmail-button'), process: document.querySelector('#process-button'),
  gmailStatus: document.querySelector('#gmail-status'), discordStatus: document.querySelector('#discord-status'),
  dialog: document.querySelector('#analysis-dialog'), analysis: document.querySelector('#analysis-content'),
  profileButton: document.querySelector('#profile-button'), profileDialog: document.querySelector('#profile-dialog'),
  profileForm: document.querySelector('#profile-form'), profileMode: document.querySelector('#profile-mode'),
  profileError: document.querySelector('#profile-error'), profileReset: document.querySelector('#profile-reset'),
  search: document.querySelector('#search-filter'), workModel: document.querySelector('#work-model-filter'),
  vacancyStatus: document.querySelector('#status-filter'), score: document.querySelector('#score-filter')
};
let allVacancies = [];

const escapeHtml = (value = '') => String(value).replace(/[&<>'"]/g, char => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', "'":'&#039;', '"':'&quot;' }[char]));
const statusLabel = status => ({ RECEBIDA:'Aguardando análise', ANALISADA:'Analisada', DESCARTADA:'Descartada' }[status] ?? status);
const formatWorkModel = model => ({ REMOTO:'Remoto', HIBRIDO:'Híbrido', PRESENCIAL:'Presencial', NAO_INFORMADO:'Modelo não informado' }[model] ?? 'Modelo não informado');
const formatPublicationDate = date => date
  ? `Publicada em ${new Intl.DateTimeFormat('pt-BR', { dateStyle:'medium' }).format(new Date(date))}`
  : '';
const csrfToken = () => document.cookie.split('; ').find(row => row.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=');

async function request(url, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const csrfHeaders = ['GET', 'HEAD', 'OPTIONS'].includes(method) || !csrfToken()
    ? {} : { 'X-XSRF-TOKEN': decodeURIComponent(csrfToken()) };
  const response = await fetch(url, { ...options, headers: { Accept:'application/json', ...csrfHeaders, ...(options.headers || {}) } });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `Não foi possível concluir a operação (${response.status}).`);
  }
  return response.status === 204 ? null : response.json();
}

const profileFields = ['objetivo', 'stackPrincipal', 'conhecimentosBasicos', 'formacao', 'experiencia', 'preferencias'];

function fillProfile(profile) {
  for (const field of profileFields) {
    document.querySelector(`#profile-${field.replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`)}`).value = profile[field] || '';
  }
  elements.profileMode.textContent = profile.personalizado
    ? 'Você está usando uma versão personalizada do perfil.'
    : 'Você está usando o perfil-base padrão do projeto.';
  elements.profileReset.disabled = !profile.personalizado;
}

async function openProfile() {
  elements.profileError.textContent = '';
  elements.profileButton.disabled = true;
  try {
    fillProfile(await request('/api/perfil'));
    elements.profileDialog.showModal();
  } catch (error) {
    elements.status.textContent = error.message;
    elements.status.classList.add('error');
  } finally { elements.profileButton.disabled = false; }
}

function updateStats(vacancies) {
  const analyzed = vacancies.filter(vacancy => vacancy.status === 'ANALISADA').length;
  elements.total.textContent = vacancies.length;
  elements.analyzed.textContent = analyzed;
  elements.waiting.textContent = vacancies.filter(vacancy => vacancy.status === 'RECEBIDA').length;
}

function renderVacancies(vacancies) {
  if (!vacancies.length) {
    elements.vacancies.replaceChildren(document.querySelector('#empty-state-template').content.cloneNode(true));
    return;
  }
  elements.vacancies.innerHTML = vacancies.map(vacancy => `
    <article class="vacancy">
      <div>
        <h3>${escapeHtml(vacancy.cargo)}</h3>
        <p class="company">${escapeHtml(vacancy.empresa)}</p>
        <p class="details">${escapeHtml(vacancy.localizacao || 'Localização não informada')} · ${formatWorkModel(vacancy.modeloTrabalho)}${formatPublicationDate(vacancy.dataPublicacao) ? ` · ${formatPublicationDate(vacancy.dataPublicacao)}` : ''}</p>
      </div>
      <div class="vacancy-actions">
        <span class="badge badge-${vacancy.status.toLowerCase()}">${statusLabel(vacancy.status)}</span>
        ${vacancy.pontuacao == null ? '' : `<span class="badge score-pill">${vacancy.pontuacao}/100</span>`}
        <button class="analyze-button" data-vacancy-id="${vacancy.id}" type="button">${vacancy.status === 'ANALISADA' ? 'Ver análise' : 'Analisar'}</button>
        ${vacancy.status === 'DESCARTADA' ? '' : `<button class="discard-button" data-discard-id="${vacancy.id}" type="button">Descartar</button>`}
      </div>
    </article>`).join('');
}

function applyFilters() {
  const term = elements.search.value.trim().toLocaleLowerCase('pt-BR');
  const score = Number(elements.score.value || 0);
  const filtered = allVacancies.filter(vacancy =>
    (!term || `${vacancy.cargo} ${vacancy.empresa}`.toLocaleLowerCase('pt-BR').includes(term)) &&
    (!elements.workModel.value || vacancy.modeloTrabalho === elements.workModel.value) &&
    (!elements.vacancyStatus.value || vacancy.status === elements.vacancyStatus.value) &&
    (!score || (vacancy.pontuacao != null && vacancy.pontuacao >= score))
  );
  renderVacancies(filtered);
  elements.status.textContent = `${filtered.length} de ${allVacancies.length} vaga(s) exibida(s).`;
}

async function loadVacancies() {
  elements.status.textContent = 'Atualizando...';
  try {
    allVacancies = await request('/api/vagas');
    updateStats(allVacancies);
    applyFilters();
  } catch (error) {
    elements.status.textContent = error.message;
    elements.status.classList.add('error');
  }
}

async function loadIntegrationStatus() {
  try {
    const status = await request('/api/integracoes/status');
    if (status.gmailConnected) {
      elements.gmail.textContent = 'Trocar conta Gmail';
      elements.gmailStatus.textContent = `Gmail conectado: ${status.gmailAccount}`;
      elements.gmailStatus.classList.remove('warning');
    } else {
      elements.gmail.textContent = 'Conectar Gmail';
      elements.gmailStatus.textContent = 'Gmail ainda não conectado.';
      elements.gmailStatus.classList.add('warning');
    }
    if (status.discordConfigured) {
      elements.discordStatus.textContent = `Discord configurado: alertas a partir de ${status.discordMinimumScore}/100.`;
      elements.discordStatus.classList.remove('warning');
    } else {
      elements.discordStatus.textContent = 'Discord não configurado: informe o webhook para receber alertas.';
      elements.discordStatus.classList.add('warning');
    }
  } catch (error) {
    elements.gmailStatus.textContent = 'Não foi possível verificar a conexão do Gmail.';
    elements.discordStatus.textContent = 'Não foi possível verificar a configuração do Discord.';
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
  const discardButton = event.target.closest('[data-discard-id]');
  if (discardButton) {
    if (!window.confirm('Descartar esta vaga? Ela continuará no histórico, mas não será considerada pendente.')) return;
    discardButton.disabled = true;
    try {
      await request(`/api/vagas/${discardButton.dataset.discardId}/descartar`, { method:'POST' });
      await loadVacancies();
    } catch (error) { elements.status.textContent = error.message; elements.status.classList.add('error'); }
    return;
  }
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
  if (!window.confirm('Buscar novas vagas e analisá-las agora? Esta ação pode consumir créditos da OpenAI.')) return;
  elements.process.disabled = true; elements.process.textContent = 'Processando...';
  try {
    const result = await request('/api/gmail/process', { method:'POST' });
    elements.status.textContent = `${result.importacao.vagasImportadas} vaga(s) importada(s) e ${result.vagasAnalisadas} analisada(s).`;
    await loadVacancies();
  } catch (error) {
    elements.status.textContent = error.message.includes('403') ? 'Conecte o Gmail antes de importar.' : error.message;
    elements.status.classList.add('error');
  } finally { elements.process.disabled = false; elements.process.textContent = 'Buscar e analisar agora'; }
});

elements.refresh.addEventListener('click', loadVacancies);
elements.profileButton.addEventListener('click', openProfile);
elements.profileForm.addEventListener('submit', async event => {
  event.preventDefault();
  elements.profileError.textContent = '';
  const submit = elements.profileForm.querySelector('[type="submit"]');
  submit.disabled = true;
  try {
    const profile = Object.fromEntries(profileFields.map(field => [field,
      document.querySelector(`#profile-${field.replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`)}`).value.trim()
    ]));
    fillProfile(await request('/api/perfil', {
      method: 'PUT', headers: { 'Content-Type':'application/json' }, body: JSON.stringify(profile)
    }));
    elements.profileMode.textContent = 'Perfil salvo. As próximas análises usarão esta versão.';
  } catch (error) { elements.profileError.textContent = error.message; }
  finally { submit.disabled = false; }
});
elements.profileReset.addEventListener('click', async () => {
  if (!window.confirm('Restaurar o perfil-base fixo do projeto? A sua versão personalizada será removida.')) return;
  elements.profileReset.disabled = true;
  elements.profileError.textContent = '';
  try {
    await request('/api/perfil', { method: 'DELETE' });
    fillProfile(await request('/api/perfil'));
    elements.profileMode.textContent = 'Perfil-base padrão restaurado.';
  } catch (error) { elements.profileError.textContent = error.message; }
});
for (const filter of [elements.search, elements.workModel, elements.vacancyStatus, elements.score]) {
  filter.addEventListener(filter === elements.search ? 'input' : 'change', applyFilters);
}
document.querySelector('#dialog-close').addEventListener('click', () => elements.dialog.close());
document.querySelector('#profile-dialog-close').addEventListener('click', () => elements.profileDialog.close());
loadVacancies();
loadIntegrationStatus();
