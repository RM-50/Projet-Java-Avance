package fr.fabrique.serveur.model;

/**
 * Résultat de la validation d'une {@link Commande}.
 * <p>
 * Utilise le pattern "result type" pour éviter les exceptions de flux de contrôle.
 *
 * @param valide  {@code true} si la commande peut être produite
 * @param erreur  message d'erreur (non null si {@code valide == false})
 */
public record ValidationResult(boolean valide, String erreur) {

    public static ValidationResult ok() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult echec(String erreur) {
        return new ValidationResult(false, erreur);
    }
}
