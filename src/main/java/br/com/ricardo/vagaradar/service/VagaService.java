package br.com.ricardo.vagaradar.service;

import br.com.ricardo.vagaradar.dto.VagaCreateRequest;
import br.com.ricardo.vagaradar.dto.VagaResponse;
import br.com.ricardo.vagaradar.dto.AnaliseVagaResponse;
import br.com.ricardo.vagaradar.dto.PaginaVagasResponse;
import br.com.ricardo.vagaradar.dto.AvaliacaoVagaRequest;
import br.com.ricardo.vagaradar.entity.AnaliseVaga;
import br.com.ricardo.vagaradar.entity.AvaliacaoUsuario;
import br.com.ricardo.vagaradar.entity.Vaga;
import br.com.ricardo.vagaradar.entity.StatusVaga;
import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import br.com.ricardo.vagaradar.exception.VagaNaoEncontradaException;
import br.com.ricardo.vagaradar.exception.VagaDuplicadaException;
import br.com.ricardo.vagaradar.integration.openai.OpenAiAnalysisResult;
import br.com.ricardo.vagaradar.integration.openai.OpenAiVagaAnalyzer;
import br.com.ricardo.vagaradar.repository.AnaliseVagaRepository;
import br.com.ricardo.vagaradar.repository.VagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VagaService {

    private final VagaRepository vagaRepository;
    private final AnaliseVagaRepository analiseVagaRepository;
    private final OpenAiVagaAnalyzer openAiVagaAnalyzer;
    private final DiscordNotificationOutboxService notificationOutboxService;

    public VagaService(
            VagaRepository vagaRepository,
            AnaliseVagaRepository analiseVagaRepository,
            OpenAiVagaAnalyzer openAiVagaAnalyzer,
            DiscordNotificationOutboxService notificationOutboxService
    ) {
        this.vagaRepository = vagaRepository;
        this.analiseVagaRepository = analiseVagaRepository;
        this.openAiVagaAnalyzer = openAiVagaAnalyzer;
        this.notificationOutboxService = notificationOutboxService;
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
    public PaginaVagasResponse listarPaginado(
            int pagina,
            String busca,
            ModeloTrabalho modeloTrabalho,
            StatusVaga status,
            AvaliacaoUsuario avaliacaoUsuario,
            Integer notaMinima
    ) {
        Page<Vaga> resultado = vagaRepository.buscarPaginado(
                normalizarBusca(busca), modeloTrabalho, status, avaliacaoUsuario, notaMinima, PageRequest.of(pagina, 25)
        );
        Map<Long, AnaliseVaga> analisesPorVaga = analiseVagaRepository.findAllByVagaIdIn(
                resultado.getContent().stream().map(Vaga::getId).toList()
        ).stream().collect(Collectors.toMap(analise -> analise.getVaga().getId(), Function.identity()));
        return new PaginaVagasResponse(
                resultado.getContent().stream()
                        .map(vaga -> paraResponseDaLista(vaga, analisesPorVaga.get(vaga.getId())))
                        .toList(),
                resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages(),
                vagaRepository.count(),
                vagaRepository.countByStatus(StatusVaga.ANALISADA),
                vagaRepository.countByStatus(StatusVaga.RECEBIDA),
                vagaRepository.countByAvaliacaoUsuario(AvaliacaoUsuario.PENDENTE),
                vagaRepository.countByAvaliacaoUsuario(AvaliacaoUsuario.GOSTEI),
                vagaRepository.countByAvaliacaoUsuario(AvaliacaoUsuario.NAO_GOSTEI)
        );
    }

    private String normalizarBusca(String busca) {
        return busca == null || busca.isBlank() ? null : busca.trim().toLowerCase(Locale.forLanguageTag("pt-BR"));
    }

    @Transactional(readOnly = true)
    public VagaResponse buscarPorId(Long id) {
        return vagaRepository.findById(id)
                .map(this::paraResponse)
                .orElseThrow(() -> new VagaNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<Long> listarIdsPendentesDeAnalise() {
        return vagaRepository.findAllByStatus(StatusVaga.RECEBIDA).stream()
                .map(Vaga::getId)
                .toList();
    }

    @Transactional
    public AnaliseVagaResponse analisar(Long id) {
        return analiseVagaRepository.findByVagaId(id)
                .map(this::paraAnaliseResponse)
                .orElseGet(() -> criarAnalise(id));
    }

    @Transactional
    public VagaResponse descartar(Long id) {
        Vaga vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new VagaNaoEncontradaException(id));
        vaga.descartar();
        return paraResponse(vaga);
    }

    @Transactional
    public VagaResponse avaliar(Long id, AvaliacaoVagaRequest request) {
        Vaga vaga = vagaRepository.findById(id)
                .orElseThrow(() -> new VagaNaoEncontradaException(id));
        vaga.avaliar(request.avaliacao(), request.motivosRejeicao(), request.outroMotivo());
        return paraResponse(vaga);
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
        if (resultado.dadosExtraidosConfiaveis()) {
            vaga.enriquecerComDadosDaAnalise(
                    resultado.localizacaoExtraida(), resultado.modeloTrabalhoExtraido(),
                    new LinkedHashSet<>(resultado.tecnologias()), new LinkedHashSet<>(resultado.habilidades()),
                    resultado.senioridade(), resultado.requisitosPrincipais()
            );
        }
        vaga.marcarComoAnalisada();
        notificationOutboxService.registrarSeElegivel(analiseSalva);
        return paraAnaliseResponse(analiseSalva);
    }

    private VagaResponse paraResponse(Vaga vaga) {
        Optional<AnaliseVaga> analise = analiseVagaRepository.findByVagaId(vaga.getId());
        return paraResponse(vaga, analise.orElse(null), Set.copyOf(vaga.getMotivosRejeicao()), vaga.getOutroMotivoRejeicao());
    }

    private VagaResponse paraResponseDaLista(Vaga vaga, AnaliseVaga analise) {
        return paraResponse(vaga, analise, Set.copyOf(vaga.getMotivosRejeicao()), vaga.getOutroMotivoRejeicao());
    }

    private VagaResponse paraResponse(
            Vaga vaga,
            AnaliseVaga analise,
            Set<br.com.ricardo.vagaradar.entity.MotivoRejeicao> motivosRejeicao,
            String outroMotivoRejeicao
    ) {
        Integer pontuacao = analise == null ? null : analise.getPontuacao();
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
                analise == null ? null : analise.getAnalisadaEm(),
                vaga.getStatus(),
                pontuacao,
                vaga.getAvaliacaoUsuario(),
                motivosRejeicao,
                outroMotivoRejeicao,
                vaga.getSenioridade(),
                vaga.getRequisitosPrincipais(),
                Set.copyOf(vaga.getTecnologias()),
                Set.copyOf(vaga.getHabilidades())
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
