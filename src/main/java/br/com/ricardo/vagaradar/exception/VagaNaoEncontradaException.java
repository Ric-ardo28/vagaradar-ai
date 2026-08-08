package br.com.ricardo.vagaradar.exception;

public class VagaNaoEncontradaException extends RuntimeException {

    public VagaNaoEncontradaException(Long id) {
        super("Vaga não encontrada para o identificador: " + id);
    }
}
