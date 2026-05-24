package fr.fabrique.usine;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implémentation de l'{@link Usine}.
 */
public class UsineImpl implements Usine {

    private static final Logger LOG = LoggerFactory.getLogger(UsineImpl.class);

    private final Fabricateur fabricateur;
    private final ReentrantLock lockConfigurer = new ReentrantLock();
    private final ExecutorService pool;
    private final Dispatcher dispatcher;

    public UsineImpl(int capacity){
        this(new Fabricateur(capacity), null);
    }

    public UsineImpl(Fabricateur fabricateur) {
        this(fabricateur, null);
    }

    public UsineImpl() {
        this(new Fabricateur(), null);
    }

    public UsineImpl(Fabricateur fabricateur, Dispatcher dispatcher) {
        this.fabricateur = fabricateur;
        this.dispatcher  = dispatcher;
        this.pool        = Executors.newFixedThreadPool(fabricateur.getCapacity());
        LOG.info("Usine initialisee — capacite={}, mode={}",
                fabricateur.getCapacity(), dispatcher != null ? "mutualise" : "sequentiel");
    }

    /**
     * {@inheritDoc}
     *
     * 0param typesLunettes Liste de lunettes à produire
     */
    @Override
    public List<Lunette> produire(Map<TypeLunette, Integer> typesLunettes) throws UsineException {
        return produire(typesLunettes, null);
    }

    @Override
    public List<Lunette> produire(Map<TypeLunette, Integer> typesLunettes,
                                  java.util.function.Consumer<String> onStatut)
            throws UsineException {
        validerCommande(typesLunettes);

        if (onStatut != null) onStatut.accept("processing");

        List<Lunette> lunettes;
        if (dispatcher != null) {
            lunettes = produireViaDispatcher(typesLunettes);
        } else {
            lunettes = produireSequentiel(typesLunettes);
        }

        if (onStatut != null) onStatut.accept("processed");
        return lunettes;
    }

    /**
     * Méthode produireSequentiel qui produit des commandes de manière séquentielle
     * @param typesLunettes la commande sous forme de map
     * @return renvoie les lunettes fabriquées
     * @throws UsineException
     */
    private List<Lunette> produireSequentiel(Map<TypeLunette, Integer> typesLunettes) throws UsineException {
        LOG.info("Production séquentielle : {}", typesLunettes);
        List<TypeLunette> file = transformerEnListe(typesLunettes);
        int total    = file.size();
        int capacity = fabricateur.getCapacity();
        LOG.debug("Total : {} lunettes en lots de {}", total, capacity);

        List<Lunette> toutes = new ArrayList<>(total);
        int debut = 0;
        while (debut < total) {
            int fin = Math.min(debut + capacity, total);
            toutes.addAll(fabriquerLotParallele(file.subList(debut, fin)));
            debut = fin;
        }
        LOG.info("Production sequentielle terminee : {} lunettes", toutes.size());
        return toutes;
    }

    /**
     * Méthode fabriquerLotParallele qui permet de fabriquer les lots de manière parallèle
     * @param lot liste des Type Lunette à fabriquer
     * @return la liste des lunettes fabriquées
     * @throws UsineException
     */
    private List<Lunette> fabriquerLotParallele(List<TypeLunette> lot) throws UsineException {
        lockConfigurer.lock();
        try {
            LOG.debug("Configuration lot {} emplacements", lot.size());
            fabricateur.configurer(lot.toArray(new TypeLunette[0]));
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new UsineException("Erreur de configuration : " + e.getMessage(), e);
        } finally {
            lockConfigurer.unlock();
        }

        // On lance la production
        List<Future<Lunette>> futures = new ArrayList<>(lot.size());
        for (TypeLunette type : lot) {
            futures.add(pool.submit(() -> {
                LOG.debug("fabriquer({}) sur {}", type, Thread.currentThread().getName());
                return fabricateur.fabriquer(type);
            }));
        }

        // On récupère les lunettes une fois produites
        List<Lunette> produites = new ArrayList<>(lot.size());
        for (Future<Lunette> future : futures) {
            try {
                produites.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UsineException("Fabrication interrompue", e);
            } catch (ExecutionException e) {
                throw new UsineException(
                        "Erreur de fabrication : " + e.getCause().getMessage(), e.getCause());
            }
        }
        return produites;
    }

    /**
     * Méthode produireViaDispatcher qui permet de mutualiser les commandes
     * @param typesLunettes commandes sous forme de map
     * @return la liste des lunettes
     * @throws UsineException
     */
    private List<Lunette> produireViaDispatcher(Map<TypeLunette, Integer> typesLunettes)
            throws UsineException {
        LOG.info("Production via dispatcher : {}", typesLunettes);

        // Transformer la commande en liste
        List<TypeLunette> file = transformerEnListe(typesLunettes);
        int capacity = fabricateur.getCapacity();

        // Découper en sous-lots de taille <= capacity et soumettre chacun
        List<CompletableFuture<List<Lunette>>> futures = new ArrayList<>();
        int debut = 0;
        while (debut < file.size()) {
            int fin = Math.min(debut + capacity, file.size());
            List<TypeLunette> sousLot = file.subList(debut, fin);

            // Reconstruire une Map pour ce sous-lot
            Map<TypeLunette, Integer> mapSousLot = new LinkedHashMap<>();
            for (TypeLunette type : sousLot) {
                mapSousLot.merge(type, 1, Integer::sum);
            }
            futures.add(dispatcher.soumettre(mapSousLot));
            debut = fin;
        }

        // Attendre tous les sous-lots et agréger
        List<Lunette> toutes = new ArrayList<>();
        for (CompletableFuture<List<Lunette>> future : futures) {
            try {
                toutes.addAll(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UsineException("Production interrompue", e);
            } catch (ExecutionException e) {
                throw new UsineException(
                        "Erreur de production : " + e.getCause().getMessage(), e.getCause());
            }
        }
        LOG.info("Production dispatcher terminee : {} lunettes", toutes.size());
        return toutes;
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
                throw new UsineException("Quantite invalide pour " + entry.getKey() + " : " + entry.getValue());
            }
        }
        long totalQte = typesLunettes.values().stream().mapToLong(Integer::longValue).sum();
        if (totalQte == 0) {
            throw new UsineException("La quantite totale est zero.");
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
}
