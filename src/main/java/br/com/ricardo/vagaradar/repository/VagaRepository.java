package br.com.ricardo.vagaradar.repository;

import br.com.ricardo.vagaradar.entity.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    Optional<Vaga> findByLinkedinId(String linkedinId);

    Optional<Vaga> findByLink(String link);
}
