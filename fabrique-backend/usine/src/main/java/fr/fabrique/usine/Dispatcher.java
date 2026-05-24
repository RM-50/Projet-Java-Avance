package fr.fabrique.usine;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dispatcher de production
 */
public class Dispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(Dispatcher.class);

    private final Fabricateur fabricateur;
    private final int capacity;
    private final ReentrantLock lockConfigurer = new ReentrantLock();
    private final ExecutorService poolFabrication;
    private final LinkedBlockingQueue<DemandeLot> file = new LinkedBlockingQueue<>();
    private final Thread dispatcherThread;
    private volatile boolean actif = true;

    public Dispatcher(Fabricateur fabricateur) {
        this.fabricateur      = fabricateur;
        this.capacity         = fabricateur.getCapacity();
        this.poolFabrication  = Executors.newFixedThreadPool(capacity);
        this.dispatcherThread = new Thread(this::boucleDispatch, "dispatcher-thread");
        this.dispatcherThread.setDaemon(true);
        this.dispatcherThread.start();
        LOG.info("Dispatcher demarre — capacite machine : {}", capacity);
    }

    public CompletableFuture<List<Lunette>> soumettre(Map<TypeLunette, Integer> typesLunettes) {
        DemandeLot demande = new DemandeLot(typesLunettes);
        file.add(demande);
        LOG.debug("Commande soumise au dispatcher ({} lunettes)", demande.total());
        return demande.future;
    }

    public void arreter() {
        actif = false;
        dispatcherThread.interrupt();
        poolFabrication.shutdown();
    }

    private void boucleDispatch() {
        while (actif) {
            try {
                DemandeLot premiere = file.take();
                List<DemandeLot> groupe = new ArrayList<>();
                groupe.add(premiere);

                int placesRestantes = capacity - premiere.total();
                DemandeLot suivante;
                while (placesRestantes > 0 && (suivante = file.poll()) != null) {
                    if (suivante.total() <= placesRestantes) {
                        groupe.add(suivante);
                        placesRestantes -= suivante.total();
                    } else {
                        file.put(suivante);
                        break;
                    }
                }

                LOG.info("Lot groupé : {} commande(s), {} lunettes au total",
                        groupe.size(), capacity - placesRestantes);

                traiterGroupe(groupe);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.info("Dispatcher interrompu, arrêt");
            }
        }
    }

    private void traiterGroupe(List<DemandeLot> groupe) {

        List<TypeLunette> tousLesTypes = new ArrayList<>();
        List<List<Integer>> indices = new ArrayList<>();

        for (DemandeLot demande : groupe) {
            List<Integer> idx = new ArrayList<>();
            for (Map.Entry<TypeLunette, Integer> e : demande.typesLunettes.entrySet()) {
                for (int i = 0; i < e.getValue(); i++) {
                    idx.add(tousLesTypes.size());
                    tousLesTypes.add(e.getKey());
                }
            }
            indices.add(idx);
        }

        lockConfigurer.lock();
        try {
            fabricateur.configurer(tousLesTypes.toArray(new TypeLunette[0]));
        } catch (Exception e) {
            LOG.error("Erreur de configuration du groupe", e);
            groupe.forEach(d -> d.future.completeExceptionally(
                    new UsineException("Erreur de configuration : " + e.getMessage(), e)));
            return;
        } finally {
            lockConfigurer.unlock();
        }

        List<Future<Lunette>> futures = new ArrayList<>(tousLesTypes.size());
        for (TypeLunette type : tousLesTypes) {
            futures.add(poolFabrication.submit(() -> fabricateur.fabriquer(type)));
        }

        List<Lunette> toutesLesLunettes = new ArrayList<>(tousLesTypes.size());
        for (Future<Lunette> f : futures) {
            try {
                toutesLesLunettes.add(f.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                groupe.forEach(d -> d.future.completeExceptionally(
                        new UsineException("Fabrication interrompue", e)));
                return;
            } catch (ExecutionException e) {
                groupe.forEach(d -> d.future.completeExceptionally(
                        new UsineException("Erreur de fabrication : " + e.getCause().getMessage(), e.getCause())));
                return;
            }
        }

        for (int i = 0; i < groupe.size(); i++) {
            List<Lunette> lunettesDeLaDemande = new ArrayList<>();
            for (int idx : indices.get(i)) {
                lunettesDeLaDemande.add(toutesLesLunettes.get(idx));
            }
            groupe.get(i).future.complete(lunettesDeLaDemande);
            LOG.debug("Commande {} completee ({} lunettes)", i, lunettesDeLaDemande.size());
        }
    }
}
