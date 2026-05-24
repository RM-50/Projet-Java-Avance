package fr.fabrique.usine;

import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface Usine {

    /**
     * Lance la production de lunettes. Chaque entrée de la {@code Map}
     * associe au type de lunette la quantité qu'il faut en produire.
     *
     * @param typesLunettes quantités demandées par type (clés non nulles, valeurs &gt; 0)
     * @return la liste des lunettes produites avec leur numéro de série
     * @throws UsineException en cas d'échec de fabrication
     */
    List<Lunette> produire(Map<TypeLunette, Integer> typesLunettes) throws UsineException;

    /**
     *
     * Variante avec listener de progression — Jalon 3.3.
     *<p>
     * Le {@code onStatut} est appelé avec :
     * <ul>
     *  <li>{@code "processing"} quand la fabrication démarre</li>
     *  <li>{@code "processed"}  quand elle se termine</li>
     * </ul>
     * Implémentation par défaut : délègue à {@link #produire(Map)} sans notifier.
     * Les implémentations peuvent surcharger pour notifier réellement.*
     * @param typesLunettes quantités demandées
     * @param onStatut      callback appelé à chaque changement d'état
     * */
    default List<Lunette> produire(Map<TypeLunette, Integer> typesLunettes, Consumer<String> onStatut) throws UsineException {
        return produire(typesLunettes);
    }
}

