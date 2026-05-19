package fr.fabrique.usine;

import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de {@link UsineImpl}.
 */
class UsineImplTest {

    // Capacité fixe
    private static final int CAPACITE = 4;

    private UsineImpl usine;

    @BeforeEach
    void setUp() {
        // capacité fixe pour que les tests ne dépendent pas du hasard
        usine = new UsineImpl(CAPACITE);
    }

    @Test
    void testProduire1RetourneListe1() throws Exception {
        List<Lunette> resultat = usine.produire(Map.of(TypeLunette.CLAUDE, 1));
        assertEquals(1, resultat.size());
    }

    @Test
    void testProduire2RetourneListe2() throws Exception {
        List<Lunette> resultat = usine.produire(Map.of(TypeLunette.BANANA, 2));
        assertEquals(2, resultat.size());
    }

    @Test
    void testProduireCapaciteRetourneBonneTaille() throws Exception {
        // 4 lunettes avec capacité 4
        List<Lunette> resultat = usine.produire(Map.of(TypeLunette.CHATGPT, CAPACITE));
        assertEquals(CAPACITE, resultat.size());
    }

    @Test
    void testProduirePlusCapaciteRetourneBonneTaille() throws Exception {
        // 5 lunettes avec capacité 4
        List<Lunette> resultat = usine.produire(Map.of(TypeLunette.CLAUDE, 5));
        assertEquals(5, resultat.size());
    }

    @Test
    void testProduirePlusieursTypesRetourneBonneTaille() throws Exception {
        Map<TypeLunette, Integer> commande = new HashMap<>();
        commande.put(TypeLunette.CLAUDE, 1);
        commande.put(TypeLunette.BANANA, 2);
        List<Lunette> resultat = usine.produire(commande);
        assertEquals(3, resultat.size());
    }

    @Test
    void testNumeroSerieNonNull() throws Exception {
        List<Lunette> resultat = usine.produire(Map.of(TypeLunette.LE_CHAT, 1));
        assertNotNull(resultat.get(0).serial);
        assertFalse(resultat.get(0).serial.isBlank());
    }

    @Test
    void testTypeLunettes() throws Exception {
        List<Lunette> resultat = usine.produire(Map.of(TypeLunette.CLAUDE, 1));
        assertEquals(TypeLunette.CLAUDE, resultat.get(0).type);
    }

    @Test
    void testExceptionCommandeNull() {
        assertThrows(UsineException.class, () -> usine.produire(null));
    }

    @Test
    void testExceptionCommandeVide() {
        assertThrows(UsineException.class, () -> usine.produire(new HashMap<>()));
    }

    @Test
    void testExceptionCommande0() {
        Map<TypeLunette, Integer> commande = new HashMap<>();
        commande.put(TypeLunette.CLAUDE, 0);
        assertThrows(UsineException.class, () -> usine.produire(commande));
    }
}