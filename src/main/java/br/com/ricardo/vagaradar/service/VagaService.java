package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import br.com.ricardo.vagaradar.dto.VagaResponse;
import br.com.ricardo.vagaradar.dto.AnaliseVagaResponse;
import br.com.ricardo.vagaradar.entity.AnaliseVaga;
import br.com.ricardo.vagaradar.entity.Vaga;
import br.com.ricardo.vagaradar.exception.VagaNaoEncontradaException;
import br.com.ricardo.vagaradar.exception.VagaDuplicadaException;
import br.com.ricardo.vagaradar.integration.openai.OpenAiAnalysisResult;
import br.com.ricardo.vagaradar.integration.openai.OpenAiVagaAnalyzer;
import br.com.ricardo.vagaradar.integration.discord.DiscordNotifier;
import br.com.ricardo.vagaradar.repository.AnaliseVagaRepository;
import br.com.ricardo.vagaradar.repository.VagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VagaService {

    private final VagaRepository vagaRepository;
    private final AnaliseVagaRepository analiseVagaRepository;
    private final OpenAiVagaAnalyzer openAiVagaAnalyzer;
    private final DiscordNotifier discordNotifier;

    public VagaService(
            VagaRepository vagaRepository,
            AnaliseVagaRepository analiseVagaRepository,
            OpenAiVagaAnalyzer openAiVagaAnalyzer,
            DiscordNotifier discordNotifier
    ) {
        this.vagaRepository = vagaRepository;
        this.analiseVagaRepository = analiseVagaRepository;
        this.openAiVagaAnalyzer = openAiVagaAnalyzer;
        this.discordNotifier = discordNotifier;
    }

    @Transactional
    public VagaResponse criar(VagaCreateRequest request) {
        verificarDuplicidade(request);
        return paraResponse(salvar(request));
    }

    @Transactional
    public Optional<VagaResponse> criarSeNova(VagaCreateRequest request) {
        if (jaExiste(request)) {
            return Optional.empty();
        }
        return Optional.of(paraResponse(salvar(request)));
    }

    private Vaga salvar(VagaCreateRequest request) {
        Vaga vaga = new Vaga(
                request.linkedinId(),
                request.cargo(),
                request.empresa(),
                request.descricao(),
                request.localizacao(),
                request.modeloTrabalho(),
                request.link(),
                request.dataPublicacao()
        );
        return vagaRepository.save(vaga);
    }

    private void verificarDuplicidade(VagaCreateRequest request) {
        if (jaExiste(request)) {
            throw new VagaDuplicadaException();
        }
    }

    private boolean jaExiste(VagaCreateRequest request) {
        boolean mesmaUrl = vagaRepository.findByLink(request.link()).isPresent();
        boolean mesmoLinkedinId = request.linkedinId() != null && !request.linkedinId().isBlank()
                && vagaRepository.findByLinkedinId(request.linkedinId()).isPresent();
        return mesmaUrl || mesmoLinkedinId;
    }

    @Transactional(readOnly = true)
    public List<VagaResponse> listar() {
        return vagaRepository.findAll().stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VagaResponse buscarPorId(Long id) {
        return vagaRepository.findById(id)
                .map(this::paraResponse)
                .orElseThrow(() -> new VagaNaoEncontradaException(id));
    }

    @Transactional
    public AnaliseVagaResponse analisar(Long id) {
        return analiseVagaRepository.findByVagaId(id)
                .map(this::paraAnaliseResponse)
                .orElseGet(() -> criarAnalise(id));
    }

    private AnaliseVagaResponse criarAnalise(Long id) {
        Vaga vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new VagaNaoEncontradaException(id));
        OpenAiAnalysisResult resultado = openAiVagaAnalyzer.analisar(vaga);
        AnaliseVaga analise = new AnaliseVaga(
                vaga,
                resultado.pontuacao(),
                resultado.nivelCompatibilidade(),
                resultado.pontosFortes(),
                resultado.pontosFaltantes(),
                resultado.recomendacao()
        );
        AnaliseVaga analiseSalva = analiseVagaRepository.save(analise);
        vaga.marcarComoAnalisada();
        discordNotifier.notificarAnalise(vaga, analiseSalva);
        return paraAnaliseResponse(analiseSalva);
    }

    private VagaResponse paraResponse(Vaga vaga) {
        return new VagaResponse(
                vaga.getId(),
                vaga.getLinkedinId(),
                vaga.getCargo(),
                vaga.getEmpresa(),
                vaga.getDescricao(),
                vaga.getLocalizacao(),
                vaga.getModeloTrabalho(),
                vaga.getLink(),
                vaga.getDataPublicacao(),
                vaga.getDataEncontrada(),
                vaga.getStatus()
        );
    }

    private AnaliseVagaResponse paraAnaliseResponse(AnaliseVaga analise) {
        return new AnaliseVagaResponse(
                analise.getId(),
                analise.getVaga().getId(),
                analise.getPontuacao(),
                analise.getNivelCompatibilidade(),
                analise.getPontosFortes(),
                analise.getPontosFaltantes(),
                analise.getRecomendacao(),
                analise.getAnalisadaEm()
        );
    }
}
