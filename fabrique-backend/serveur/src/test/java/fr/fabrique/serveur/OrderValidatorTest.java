package fr.fabrique.serveur;

import bernard_flou.Fabricateur.TypeLunette;
import fr.fabrique.serveur.model.Commande;
import fr.fabrique.serveur.model.OrderValidator;
import fr.fabrique.serveur.model.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de {@link OrderValidator}
 */
@DisplayName("OrderValidator — règles métier")
class OrderValidatorTest {

    private static final String ID = UUID.randomUUID().toString();

    @Nested
    @DisplayName("Commandes valides")
    class Valides {

        @Test
        @DisplayName("Commande mono-type avec quantité 1 → valide")
        void monoTypeQte1Valide() {
            Commande c = new Commande(ID, Map.of(TypeLunette.CLAUDE, 1));
            assertTrue(OrderValidator.valider(c).valide());
        }

        @Test
        @DisplayName("Commande multi-types → valide")
        void multiTypesValide() {
            Commande c = new Commande(ID, Map.of(
                    TypeLunette.CLAUDE, 3,
                    TypeLunette.BANANA, 2,
                    TypeLunette.CHATGPT, 1));
            assertTrue(OrderValidator.valider(c).valide());
        }

        @Test
        @DisplayName("Quantité 9 (borne haute inclusive) → valide")
        void qte9Valide() {
            Commande c = new Commande(ID, Map.of(TypeLunette.LE_CHAT, 9));
            assertTrue(OrderValidator.valider(c).valide());
        }

        @Test
        @DisplayName("Quantité 0 pour un type mais total > 0 → valide")
        void qteZeroPourUnTypeMaisTotalPositif() {
            Commande c = new Commande(ID, Map.of(
                    TypeLunette.CLAUDE, 0,
                    TypeLunette.BANANA, 1));
            assertTrue(OrderValidator.valider(c).valide());
        }

        @Test
        @DisplayName("Tous les 4 types présents → valide")
        void tousTypesPresentsValide() {
            Commande c = new Commande(ID, Map.of(
                    TypeLunette.CLAUDE, 1,
                    TypeLunette.BANANA, 1,
                    TypeLunette.CHATGPT, 1,
                    TypeLunette.LE_CHAT, 1));
            assertTrue(OrderValidator.valider(c).valide());
        }
    }

    @Nested
    @DisplayName("Commande nulle ou vide")
    class NulleOuVide {

        @Test
        @DisplayName("Commande nulle → invalide")
        void commandeNullInvalide() {
            ValidationResult r = OrderValidator.valider(null);
            assertFalse(r.valide());
            assertNotNull(r.erreur());
        }

        @Test
        @DisplayName("Map vide → invalide")
        void mapVideInvalide() {
            Commande c = new Commande(ID, Map.of());
            ValidationResult r = OrderValidator.valider(c);
            assertFalse(r.valide());
        }
    }

    @Nested
    @DisplayName("Règle 2 : total strictement positif")
    class TotalPositif {

        @Test
        @DisplayName("Tous à zéro → invalide (total = 0)")
        void tousQteZeroInvalide() {
            Commande c = new Commande(ID, Map.of(
                    TypeLunette.CLAUDE, 0,
                    TypeLunette.BANANA, 0));
            ValidationResult r = OrderValidator.valider(c);
            assertFalse(r.valide());
            assertTrue(r.erreur().toLowerCase().contains("zéro"));
        }
    }

    @Nested
    @DisplayName("Règle 3 : quantité dans [0, 10[")
    class QuantiteValide {

        @ParameterizedTest(name = "Quantité {0} → invalide")
        @ValueSource(ints = {10, 11, 50, Integer.MAX_VALUE})
        @DisplayName("Quantité >= 10 → invalide")
        void qteHorsBorneInvalide(int qte) {
            Commande c = new Commande(ID, Map.of(TypeLunette.CLAUDE, qte));
            ValidationResult r = OrderValidator.valider(c);
            assertFalse(r.valide());
            assertTrue(r.erreur().toLowerCase().contains("quantité"));
        }

        @Test
        @DisplayName("Quantité négative → invalide")
        void qteNegativeInvalide() {
            Commande c = new Commande(ID, Map.of(TypeLunette.CLAUDE, -1));
            ValidationResult r = OrderValidator.valider(c);
            assertFalse(r.valide());
        }
    }

    @Nested
    @DisplayName("Messages d'erreur")
    class Messages {

        @Test
        @DisplayName("Le message mentionne le type incriminé")
        void messageMentionneTypeQteInvalide() {
            Commande c = new Commande(ID, Map.of(TypeLunette.BANANA, 15));
            ValidationResult r = OrderValidator.valider(c);
            assertTrue(r.erreur().toUpperCase().contains("BANANA"));
        }

        @Test
        @DisplayName("ValidationResult ok() : erreur est null")
        void okErreurNull() {
            Commande c = new Commande(ID, Map.of(TypeLunette.CLAUDE, 1));
            assertNull(OrderValidator.valider(c).erreur());
        }
    }
}
