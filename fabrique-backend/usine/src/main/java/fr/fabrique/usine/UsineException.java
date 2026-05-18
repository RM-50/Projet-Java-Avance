package fr.fabrique.usine;

/**
 * Exception levée lorsqu'une production échoue.
 */
public class UsineException extends Exception {

    public UsineException(String message) {
        super(message);
    }

    public UsineException(String message, Throwable cause) {
        super(message, cause);
    }
}
