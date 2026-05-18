package fr.fabrique.usine;

import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;

import java.util.List;
import java.util.Map;

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
}
