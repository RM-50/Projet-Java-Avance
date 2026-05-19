package fr.fabrique.usine;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implémentation de l'{@link Usine}.
 */
public class UsineImpl implements Usine {

    private static final Logger LOG = LoggerFactory.getLogger(UsineImpl.class);

    private final Fabricateur fabricateur;

    public UsineImpl(int capacity){
        this.fabricateur = new Fabricateur(capacity);
    }

    public UsineImpl(Fabricateur fabricateur) {
        this.fabricateur = fabricateur;
        LOG.info("Usine initialisée avec une capacité de {}", fabricateur.getCapacity());
    }

    public UsineImpl() {
        this(new Fabricateur());
    }

    /**
     * {@inheritDoc}
     *
     * 0param typesLunettes Liste de lunettes à produire
     */
    @Override
    public List<Lunette> produire(Map<TypeLunette, Integer> typesLunettes) throws UsineException {
        LOG.info("Démarrage production : {}", typesLunettes);
        validerCommande(typesLunettes);

        List<TypeLunette> file = transformerEnListe(typesLunettes);
        int total = file.size();
        int capacity = fabricateur.getCapacity();
        LOG.debug("Total à produire : {} en lots de {}", total, capacity);

        List<Lunette> lunettes = new ArrayList<>(total);

        // Découpage en lots
        int debut = 0;
        while (debut < total) {
            int fin = Math.min(debut + capacity, total);
            List<TypeLunette> lot = file.subList(debut, fin);
            lunettes.addAll(fabriquerLot(lot));
            debut = fin;
        }

        LOG.info("Production terminée : {} lunettes fabriquées", lunettes.size());
        return lunettes;
    }

    /**
     * Vérifie que la commande est cohérente avant de démarrer la production.
     */
    private void validerCommande(Map<TypeLunette, Integer> typesLunettes) throws UsineException {
        if (typesLunettes == null || typesLunettes.isEmpty()) {
            throw new UsineException("La commande est vide.");
        }
        for (Map.Entry<TypeLunette, Integer> entry : typesLunettes.entrySet()) {
            if (entry.getKey() == null) {
                throw new UsineException("Type de lunette null dans la commande.");
            }
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new UsineException("Quantité invalide pour " + entry.getKey() + " : " + entry.getValue());
            }
        }
        long totalQte = typesLunettes.values().stream().mapToLong(Integer::longValue).sum();
        if (totalQte == 0) {
            throw new UsineException("La quantité totale est zéro.");
        }
    }

    /**
     * Transforme une Map en liste ordonnée par type.
     * Ex. : {CLAUDE=2, BANANA=1} → [CLAUDE, CLAUDE, BANANA]
     */
    private List<TypeLunette> transformerEnListe(Map<TypeLunette, Integer> typesLunettes) {
        List<TypeLunette> result = new ArrayList<>();
        for (Map.Entry<TypeLunette, Integer> entry : typesLunettes.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                result.add(entry.getKey());
            }
        }
        return result;
    }


    /**
     * Configure la machine pour un lot, puis déclenche la fabrication de chaque lunette.
     */
    private List<Lunette> fabriquerLot(List<TypeLunette> lot) throws UsineException {
        TypeLunette[] tableau = lot.toArray(new TypeLunette[0]);
        LOG.debug("Configuration du lot : {}", lot);
        try {
            fabricateur.configurer(tableau);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new UsineException("Erreur lors de la configuration de la machine : " + e.getMessage(), e);
        }

        // Fabrication séquentielle des lunettes du lot
        List<Lunette> produites = new ArrayList<>(lot.size());
        for (TypeLunette type : lot) {
            LOG.debug("Fabrication d'une lunette {}", type);
            try {
                Lunette lunette = fabricateur.fabriquer(type);
                produites.add(lunette);
                LOG.debug("Lunette produite : {} / {}", lunette.type, lunette.serial);
            } catch (IllegalStateException | IllegalArgumentException e) {
                throw new UsineException("Erreur de fabrication pour le type " + type + " : " + e.getMessage(), e);
            }
        }
        return produites;
    }
}
