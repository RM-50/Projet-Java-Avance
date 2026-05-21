package fr.fabrique.serveur.model;

import bernard_flou.Fabricateur.TypeLunette;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Valide une {@link Commande} selon les règles métier du cahier des charges :
 * <ul>
 *   <li>Le type de lunette doit être connu (appartenir à l'enum {@link TypeLunette})</li>
 *   <li>La quantité totale doit être strictement supérieure à zéro</li>
 *   <li>La quantité de chaque type doit être comprise entre 0 (inclus) et 10 (exclu)</li>
 * </ul>
 */
public final class OrderValidator {

    /** Ensemble des types connus à la date de compilation. */
    private static final Set<TypeLunette> TYPES_CONNUS =
            EnumSet.allOf(TypeLunette.class);

    private static final int QTE_MAX_EXCLUSIVE = 10;

    private OrderValidator() { /* utilitaire statique */ }

    /**
     * Valide la commande.
     *
     * @param commande la commande à valider (non nulle)
     * @return un {@link ValidationResult} décrivant le résultat
     */
    public static ValidationResult valider(Commande commande) {
        if (commande == null) {
            return ValidationResult.echec("La commande est nulle.");
        }
        Map<TypeLunette, Integer> qtes = commande.typesQuantites();

        if (qtes == null || qtes.isEmpty()) {
            return ValidationResult.echec("La commande ne contient aucun article.");
        }

        for (Map.Entry<TypeLunette, Integer> entry : qtes.entrySet()) {
            TypeLunette type = entry.getKey();
            Integer qte = entry.getValue();

            // Règle 1 : type connu
            if (type == null || !TYPES_CONNUS.contains(type)) {
                return ValidationResult.echec(
                        "Type de lunette inconnu : " + type);
            }

            // Règle 3 : quantité dans [0, 10[
            if (qte == null || qte < 0 || qte >= QTE_MAX_EXCLUSIVE) {
                return ValidationResult.echec(
                        "Quantité invalide pour " + type + " : " + qte
                        + " (attendu : 0 ≤ qté < " + QTE_MAX_EXCLUSIVE + ")");
            }
        }

        // Règle 2 : total > 0
        if (commande.totalQuantite() == 0) {
            return ValidationResult.echec(
                    "La quantité totale est zéro : rien à produire.");
        }

        return ValidationResult.ok();
    }
}
