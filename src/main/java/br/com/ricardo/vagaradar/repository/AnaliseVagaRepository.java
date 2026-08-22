package br.com.ricardo.vagaradar.repository;

import br.com.ricardo.vagaradar.entity.AnaliseVaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnaliseVagaRepository extends JpaRepository<AnaliseVaga, Long> {

    Optional<AnaliseVaga> findByVagaId(Long vagaId);

    List<AnaliseVaga> findAllByVagaIdIn(Collection<Long> vagaIds);

    Optional<AnaliseVaga> findTopByOrderByAnalisadaEmDesc();
}
