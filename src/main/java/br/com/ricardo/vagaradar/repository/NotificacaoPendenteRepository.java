package br.com.ricardo.vagaradar.repository;

import br.com.ricardo.vagaradar.entity.NotificacaoPendente;
import br.com.ricardo.vagaradar.entity.StatusNotificacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificacaoPendenteRepository extends JpaRepository<NotificacaoPendente, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n from NotificacaoPendente n
            join fetch n.analise a
            join fetch a.vaga
            where n.status = :status and n.proximaTentativaEm <= :agora
            order by n.criadaEm asc
            """)
    List<NotificacaoPendente> buscarPendentesParaEnvio(
            @Param("status") StatusNotificacao status,
            @Param("agora") Instant agora,
            Pageable pageable
    );
}
