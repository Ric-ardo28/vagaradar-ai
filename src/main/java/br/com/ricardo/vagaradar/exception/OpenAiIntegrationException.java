package br.com.ricardo.vagaradar.exception;

public class OpenAiIntegrationException extends RuntimeException {

    public OpenAiIntegrationException(String message) {
        super(message);
    }

    public OpenAiIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
