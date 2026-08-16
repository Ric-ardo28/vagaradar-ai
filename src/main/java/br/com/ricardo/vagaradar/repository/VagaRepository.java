package br.com.ricardo.vagaradar.repository;

import br.com.ricardo.vagaradar.entity.Vaga;
import br.com.ricardo.vagaradar.entity.StatusVaga;
import br.com.ricardo.vagaradar.entity.ModeloTrabalho;
import br.com.ricardo.vagaradar.dto.VagaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    Optional<Vaga> findByLinkedinId(String linkedinId);

    Optional<Vaga> findByLink(String link);

    List<Vaga> findAllByStatus(StatusVaga status);

    long countByStatus(StatusVaga status);

    List<Vaga> findAllByOrderByDataEncontradaDesc();

    @Query(value = """
            select new br.com.ricardo.vagaradar.dto.VagaResponse(
                v.id, v.linkedinId, v.cargo, v.empresa, v.descricao, v.localizacao,
                v.modeloTrabalho, v.link, v.dataPublicacao, v.dataEncontrada, v.status, a.pontuacao
            )
            from Vaga v
            left join AnaliseVaga a on a.vaga = v
            where (:busca is null or lower(v.cargo) like concat('%', :busca, '%')
                   or lower(v.empresa) like concat('%', :busca, '%'))
              and (:modeloTrabalho is null or v.modeloTrabalho = :modeloTrabalho)
              and (:status is null or v.status = :status)
              and (:notaMinima is null or a.pontuacao >= :notaMinima)
            order by v.dataEncontrada desc, v.id desc
            """,
            countQuery = """
                    select count(v)
                    from Vaga v
                    left join AnaliseVaga a on a.vaga = v
                    where (:busca is null or lower(v.cargo) like concat('%', :busca, '%')
                           or lower(v.empresa) like concat('%', :busca, '%'))
                      and (:modeloTrabalho is null or v.modeloTrabalho = :modeloTrabalho)
                      and (:status is null or v.status = :status)
                      and (:notaMinima is null or a.pontuacao >= :notaMinima)
                    """)
    Page<VagaResponse> buscarPaginado(
            @Param("busca") String busca,
            @Param("modeloTrabalho") ModeloTrabalho modeloTrabalho,
            @Param("status") StatusVaga status,
            @Param("notaMinima") Integer notaMinima,
            Pageable pageable
    );
}
