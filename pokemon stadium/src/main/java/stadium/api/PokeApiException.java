package stadium.api;

/** Error al consultar PokeAPI (no encontrado, red, JSON inválido). El mensaje es apto para mostrar al usuario. */
public class PokeApiException extends Exception {
    public PokeApiException(String message) { super(message); }
    public PokeApiException(String message, Throwable cause) { super(message, cause); }
}
