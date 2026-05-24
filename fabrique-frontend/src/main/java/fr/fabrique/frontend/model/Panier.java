package fr.fabrique.frontend.model;

import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Panier de commande.
 */
public class Panier {

    private final ReadOnlyMapWrapper<String, Integer> quantites =
            new ReadOnlyMapWrapper<>(FXCollections.observableHashMap());

    /**
     * Méthode incrementer qui sert à ajouter un produit dans le panier
     * @param produitId
     */
    public void incrementer(String produitId) {
        quantites.put(produitId, quantites.getOrDefault(produitId, 0) + 1);
    }

    /**
     * Méthode décrémenter qui retire un produit du panier
     * @param produitId
     */
    public void decrementer(String produitId) {
        int actuel = quantites.getOrDefault(produitId, 0);
        if (actuel <= 1) {
            quantites.remove(produitId);
        } else {
            quantites.put(produitId, actuel - 1);
        }
    }

    public void vider() {
        quantites.clear();
    }

    public int quantite(String produitId) {
        return quantites.getOrDefault(produitId, 0);
    }

    public int total() {
        return quantites.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean estVide() {
        return total() == 0;
    }

    public ObservableMap<String, Integer> getQuantites() {
        return quantites.get();
    }

    public ReadOnlyMapProperty<String, Integer> quantitesProperty() {
        return quantites.getReadOnlyProperty();
    }
}
