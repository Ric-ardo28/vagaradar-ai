package br.com.ricardo.vagaradar.exception;

public class VagaDuplicadaException extends RuntimeException {

    public VagaDuplicadaException() {
        super("Esta vaga já foi cadastrada.");
    }
}
