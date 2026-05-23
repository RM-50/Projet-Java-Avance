package fr.fabrique.usine;

import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Représente une commande en attente de production dans la file du {@link Dispatcher}.
 */
class DemandeLot {

    final Map<TypeLunette, Integer> typesLunettes;
    final CompletableFuture<List<Lunette>> future;

    DemandeLot(Map<TypeLunette, Integer> typesLunettes) {
        this.typesLunettes = typesLunettes;
        this.future        = new CompletableFuture<>();
    }

    /** Nombre total de lunettes demandées dans cette commande. */
    int total() {
        return typesLunettes.values().stream().mapToInt(Integer::intValue).sum();
    }
}
