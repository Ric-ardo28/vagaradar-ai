const elements = {
  vacancies: document.querySelector('#vacancies'), status: document.querySelector('#status-message'),
  total: document.querySelector('#total-count'), analyzed: document.querySelector('#analyzed-count'),
  waiting: document.querySelector('#new-count'), refresh: document.querySelector('#refresh-button'),
  gmail: document.querySelector('#gmail-button'), process: document.querySelector('#process-button'),
  gmailStatus: document.querySelector('#gmail-status'), discordStatus: document.querySelector('#discord-status'),
  lastAnalysisStatus: document.querySelector('#last-analysis-status'),
  dialog: document.querySelector('#analysis-dialog'), analysis: document.querySelector('#analysis-content'),
  profileButton: document.querySelector('#profile-button'), profileDialog: document.querySelector('#profile-dialog'),
  profileForm: document.querySelector('#profile-form'), profileMode: document.querySelector('#profile-mode'),
  profileError: document.querySelector('#profile-error'), profileReset: document.querySelector('#profile-reset'),
  search: document.querySelector('#search-filter'), workModel: document.querySelector('#work-model-filter'),
  vacancyStatus: document.querySelector('#status-filter'), score: document.querySelector('#score-filter'),
  pagination: document.querySelector('#pagination'), previousPage: document.querySelector('#previous-page'),
  nextPage: document.querySelector('#next-page'), paginationSummary: document.querySelector('#pagination-summary'),
  vacanciesHeading: document.querySelector('#vacancies-heading'),
  evaluationTabs: document.querySelectorAll('[data-evaluation-tab]'), pendingEvaluationCount: document.querySelector('#pending-evaluation-count'),
  likedCount: document.querySelector('#liked-count'), dislikedCount: document.querySelector('#disliked-count'),
  evaluationDialog: document.querySelector('#evaluation-dialog'), evaluationForm: document.querySelector('#evaluation-form'),
  evaluationReset: document.querySelector('#evaluation-reset'), otherReasonCheckbox: document.querySelector('#other-reason-checkbox'),
  otherReasonField: document.querySelector('#other-reason-field'), otherReason: document.querySelector('#other-reason')
};
let totalVacancies = 0;
let currentPage = 1;
const pageSize = 25;
let searchTimer;
let selectedEvaluation = 'PENDENTE';
let evaluationVacancyId;
const vacancyPageCache = new Map();

const escapeHtml = (value = '') => String(value).replace(/[&<>'"]/g, char => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', "'":'&#039;', '"':'&quot;' }[char]));
const statusLabel = status => ({ RECEBIDA:'Aguardando análise', ANALISADA:'Analisada', DESCARTADA:'Descartada' }[status] ?? status);
const formatWorkModel = model => ({ REMOTO:'Remoto', HIBRIDO:'Híbrido', PRESENCIAL:'Presencial', NAO_INFORMADO:'Modelo não informado' }[model] ?? 'Modelo não informado');
const formatAnalysisDate = date => date
  ? `Analisada em ${new Intl.DateTimeFormat('pt-BR', { dateStyle:'short', timeStyle:'short' }).format(new Date(date))}`
  : '';
const formatLastAnalysis = date => new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short', timeStyle: 'short'
}).format(new Date(date));
const rejectionReasonLabel = reason => ({
  INGLES_AVANCADO:'Inglês avançado exigido', SENIORIDADE:'Senioridade acima do meu nível', REQUISITOS_EXCESSIVOS:'Muitos requisitos', TECNOLOGIAS:'Tecnologias não compatíveis',
  LOCALIZACAO:'Localização não me atende', MODELO_TRABALHO:'Modalidade não me atende', SALARIO:'Salário não me atende', TIPO_CONTRATO:'Tipo de contrato não me atende',
  AREA_ATUACAO:'Área de atuação não me interessa', EMPRESA:'Não tenho interesse na empresa', BENEFICIOS:'Benefícios insuficientes', CARGA_HORARIA:'Carga horária ou escala não me atende',
  VAGA_NAO_CLARA:'Vaga pouco clara', VAGA_DUPLICADA:'Vaga duplicada', OUTRO:'Outro motivo'
}[reason] ?? reason);
const tags = (values = [], className) => values.length ? `<div class="vacancy-tags ${className}">${values.slice(0, 4).map(value => `<span>${escapeHtml(value)}</span>`).join('')}${values.length > 4 ? `<span>+${values.length - 4}</span>` : ''}</div>` : '';
const rejectionReasons = vacancy => {
  if (vacancy.avaliacaoUsuario !== 'NAO_GOSTEI') return '';
  const reasons = (vacancy.motivosRejeicao || []).map(rejectionReasonLabel);
  if (vacancy.outroMotivoRejeicao) reasons.push(vacancy.outroMotivoRejeicao);
  return reasons.length ? `<section class="rejection-reasons"><strong>Motivo de não ter gostado</strong><p>${reasons.slice(0, 2).map(escapeHtml).join(' · ')}${reasons.length > 2 ? ` · +${reasons.length - 2} outros` : ''}</p></section>` : '';
};
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

function updateStats(page) {
  elements.total.textContent = page.totalMonitoradas;
  elements.analyzed.textContent = page.totalAnalisadas;
  elements.waiting.textContent = page.totalPendentes;
  elements.pendingEvaluationCount.textContent = page.totalPendentesAvaliacao;
  elements.likedCount.textContent = page.totalGostei;
  elements.dislikedCount.textContent = page.totalNaoGostei;
}

function renderVacancies(vacancies) {
  if (!vacancies.length) {
    elements.vacancies.replaceChildren(document.querySelector('#empty-state-template').content.cloneNode(true));
    return;
  }
  elements.vacancies.innerHTML = vacancies.map(vacancy => `
    <article class="vacancy">
      <div class="vacancy-main">
        <span class="vacancy-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="7" width="18" height="13" rx="2"></rect><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 12h18M10 12v2h4v-2"></path></svg></span>
        <div>
        <h3>${escapeHtml(vacancy.cargo)}</h3>
        <p class="company">${escapeHtml(vacancy.empresa)}</p>
        <p class="details">${escapeHtml(vacancy.localizacao || 'Localização não informada')} · ${formatWorkModel(vacancy.modeloTrabalho)}${formatAnalysisDate(vacancy.analisadaEm) ? ` · ${formatAnalysisDate(vacancy.analisadaEm)}` : ''}</p>
        ${vacancy.senioridade ? `<p class="details">${escapeHtml(vacancy.senioridade)}</p>` : ''}
        ${tags(vacancy.tecnologias, 'technology-tags')}
        ${tags(vacancy.habilidades, 'skill-tags')}
        ${vacancy.requisitosPrincipais ? `<p class="vacancy-requirements">${escapeHtml(vacancy.requisitosPrincipais)}</p>` : ''}
        ${rejectionReasons(vacancy)}
        </div>
      </div>
      <div class="vacancy-actions">
        <span class="badge badge-${vacancy.status.toLowerCase()}">${statusLabel(vacancy.status)}</span>
        ${vacancy.pontuacao == null ? '' : `<span class="badge score-pill">${vacancy.pontuacao}/100</span>`}
        <a class="vacancy-link" href="${escapeHtml(vacancy.link)}" target="_blank" rel="noopener noreferrer">Ver vaga</a>
        <button class="analyze-button" data-vacancy-id="${vacancy.id}" type="button">${vacancy.status === 'ANALISADA' ? 'Ver análise' : 'Analisar'}</button>
        <button class="evaluation-button like" data-evaluation-id="${vacancy.id}" data-evaluation="GOSTEI" type="button" title="Gostei desta vaga" aria-label="Gostei desta vaga">👍</button>
        <button class="evaluation-button dislike" data-evaluation-id="${vacancy.id}" data-evaluation="NAO_GOSTEI" type="button" title="Não gostei desta vaga" aria-label="Não gostei desta vaga">👎</button>
      </div>
    </article>`).join('');
}

function applyFilters() {
  currentPage = 1;
  loadVacancies();
}

async function loadVacancies({ force = false } = {}) {
  elements.status.textContent = 'Atualizando...';
  try {
    const params = new URLSearchParams({ pagina: String(currentPage - 1) });
    if (elements.search.value.trim()) params.set('busca', elements.search.value.trim());
    if (elements.workModel.value) params.set('modeloTrabalho', elements.workModel.value);
    if (elements.vacancyStatus.value) params.set('status', elements.vacancyStatus.value);
    if (selectedEvaluation) params.set('avaliacaoUsuario', selectedEvaluation);
    if (elements.score.value) params.set('notaMinima', elements.score.value);
    const cacheKey = params.toString();
    let page = force ? null : vacancyPageCache.get(cacheKey);
    if (!page) {
      page = await request(`/api/vagas?${params}`);
      vacancyPageCache.set(cacheKey, page);
    }
    totalVacancies = page.totalElementos;
    currentPage = page.pagina + 1;
    renderVacancies(page.vagas);
    updateStats(page);
    elements.pagination.hidden = page.totalPaginas <= 1;
    elements.previousPage.disabled = currentPage === 1;
    elements.nextPage.disabled = currentPage === page.totalPaginas;
    const first = totalVacancies ? ((currentPage - 1) * pageSize) + 1 : 0;
    const last = Math.min(currentPage * pageSize, totalVacancies);
    elements.paginationSummary.textContent = totalVacancies
      ? `${first}–${last} de ${totalVacancies} vagas · Página ${currentPage} de ${page.totalPaginas}`
      : '';
    elements.status.textContent = `${totalVacancies} vaga(s) encontrada(s).`;
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
    elements.lastAnalysisStatus.textContent = status.ultimaAnaliseEm
      ? `Última análise: ${formatLastAnalysis(status.ultimaAnaliseEm)}.`
      : 'Última análise: nenhuma vaga analisada ainda.';
  } catch (error) {
    elements.gmailStatus.textContent = 'Não foi possível verificar a conexão do Gmail.';
    elements.discordStatus.textContent = 'Não foi possível verificar a configuração do Discord.';
    elements.lastAnalysisStatus.textContent = 'Não foi possível verificar a última análise.';
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
  const evaluationButton = event.target.closest('[data-evaluation-id]');
  if (evaluationButton) {
    if (evaluationButton.dataset.evaluation === 'NAO_GOSTEI') {
      evaluationVacancyId = evaluationButton.dataset.evaluationId;
      elements.evaluationForm.reset();
      elements.otherReasonField.hidden = true;
      elements.evaluationDialog.showModal();
      return;
    }
    evaluationButton.disabled = true;
    try {
      await saveEvaluation(evaluationButton.dataset.evaluationId, { avaliacao:'GOSTEI' });
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
    await loadIntegrationStatus();
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
    vacancyPageCache.clear();
    await loadVacancies({ force:true });
    await loadIntegrationStatus();
  } catch (error) {
    elements.status.textContent = error.message.includes('403') ? 'Conecte o Gmail antes de importar.' : error.message;
    elements.status.classList.add('error');
  } finally { elements.process.disabled = false; elements.process.textContent = 'Buscar e analisar agora'; }
});

elements.refresh.addEventListener('click', () => loadVacancies({ force:true }));
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
elements.search.addEventListener('input', () => {
  window.clearTimeout(searchTimer);
  searchTimer = window.setTimeout(applyFilters, 300);
});

async function saveEvaluation(id, body) {
  await request(`/api/vagas/${id}/avaliacao`, {
    method:'POST', headers: { 'Content-Type':'application/json' }, body: JSON.stringify(body)
  });
  vacancyPageCache.clear();
  await loadVacancies();
}

elements.evaluationForm.addEventListener('submit', async event => {
  event.preventDefault();
  const submit = elements.evaluationForm.querySelector('[type="submit"]');
  submit.disabled = true;
  try {
    const motivosRejeicao = [...elements.evaluationForm.querySelectorAll('[name="reason"]:checked')].map(input => input.value);
    await saveEvaluation(evaluationVacancyId, {
      avaliacao:'NAO_GOSTEI', motivosRejeicao,
      outroMotivo: elements.otherReason.value.trim() || null
    });
    elements.evaluationDialog.close();
  } catch (error) { elements.status.textContent = error.message; elements.status.classList.add('error'); }
  finally { submit.disabled = false; }
});

elements.otherReasonCheckbox.addEventListener('change', () => {
  elements.otherReasonField.hidden = !elements.otherReasonCheckbox.checked;
  if (!elements.otherReasonCheckbox.checked) elements.otherReason.value = '';
});

elements.evaluationReset.addEventListener('click', async () => {
  elements.evaluationReset.disabled = true;
  try {
    await saveEvaluation(evaluationVacancyId, { avaliacao:'PENDENTE' });
    elements.evaluationDialog.close();
  } catch (error) { elements.status.textContent = error.message; elements.status.classList.add('error'); }
  finally { elements.evaluationReset.disabled = false; }
});
for (const filter of [elements.workModel, elements.vacancyStatus, elements.score]) {
  filter.addEventListener('change', applyFilters);
}
for (const tab of elements.evaluationTabs) {
  tab.addEventListener('click', () => {
    selectedEvaluation = tab.dataset.evaluationTab;
    elements.vacanciesHeading.textContent = ({ PENDENTE:'Vagas encontradas', GOSTEI:'Vagas que gostei', NAO_GOSTEI:'Vagas que não gostei' })[selectedEvaluation];
    elements.evaluationTabs.forEach(item => {
      const active = item === tab;
      item.classList.toggle('is-active', active);
      item.setAttribute('aria-selected', String(active));
    });
    applyFilters();
  });
}
elements.previousPage.addEventListener('click', () => {
  currentPage--;
  loadVacancies();
});
elements.nextPage.addEventListener('click', () => {
  currentPage++;
  loadVacancies();
});
document.querySelector('#dialog-close').addEventListener('click', () => elements.dialog.close());
document.querySelector('#evaluation-dialog-close').addEventListener('click', () => elements.evaluationDialog.close());
document.querySelector('#profile-dialog-close').addEventListener('click', () => elements.profileDialog.close());
loadVacancies();
loadIntegrationStatus();
