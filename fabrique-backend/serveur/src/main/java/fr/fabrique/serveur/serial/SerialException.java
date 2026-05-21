package fr.fabrique.serveur.serial;

/** Levée lorsqu'un payload ne respecte pas le format attendu. */
public class SerialException extends Exception {
    public SerialException(String message) { super(message); }
    public SerialException(String message, Throwable cause) { super(message, cause); }
}
