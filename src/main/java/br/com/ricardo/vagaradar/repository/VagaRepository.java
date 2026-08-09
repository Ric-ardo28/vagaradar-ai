package br.com.ricardo.vagaradar.repository;

import br.com.ricardo.vagaradar.entity.Vaga;
import br.com.ricardo.vagaradar.entity.StatusVaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    Optional<Vaga> findByLinkedinId(String linkedinId);

    Optional<Vaga> findByLink(String link);

    List<Vaga> findAllByStatus(StatusVaga status);
}
