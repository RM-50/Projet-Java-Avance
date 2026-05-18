package fr.fabrique.usine;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Implémentation de l'{@link Usine}.
 */
public class UsineImpl implements Usine {

    private static final Logger LOG = LoggerFactory.getLogger(UsineImpl.class);

    private final Fabricateur fabricateur;

    public UsineImpl(Fabricateur fabricateur) {
        this.fabricateur = fabricateur;
        LOG.info("Usine initialisée avec une capacité de {}", fabricateur.getCapacity());
    }

    public UsineImpl() {
        this(new Fabricateur());
    }

    @Override
    public List<Lunette> produire(Map<TypeLunette, Integer> typesLunettes) throws UsineException {
        throw new UnsupportedOperationException("À implémenter");
    }
}
