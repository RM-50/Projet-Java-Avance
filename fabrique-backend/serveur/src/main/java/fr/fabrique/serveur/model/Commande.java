package fr.fabrique.serveur.model;

import bernard_flou.Fabricateur.TypeLunette;

import java.util.Collections;
import java.util.Map;

/**
 * Représente une commande passée par un client.
 * <p>
 * Immuable : la map des quantités ne peut pas être modifiée après construction.
 *
 * @param orderId        Identifiant unique (UUID) de la commande
 * @param typesQuantites Association TypeLunette -> quantité désirée
 */
public record Commande(String orderId, Map<TypeLunette, Integer> typesQuantites) {

    public Commande {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId ne peut pas être vide");
        }
        typesQuantites = Collections.unmodifiableMap(typesQuantites);
    }

    // Nombre total de lunettes à produire.
    public int totalQuantite() {
        return typesQuantites.values().stream().mapToInt(Integer::intValue).sum();
    }
}
