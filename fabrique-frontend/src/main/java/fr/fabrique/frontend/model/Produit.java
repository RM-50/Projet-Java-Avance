package fr.fabrique.frontend.model;

/**
 * Représente un produit du catalogue comme dans {@code products.json}.
 *
 * @param id          identifiant technique
 * @param name        nom affiché
 * @param price       prix en euros
 * @param badge       badge promotionnel
 * @param description description marketing courte
 */
public record Produit(
        String id,
        String name,
        double price,
        String badge,
        String description
) {}
