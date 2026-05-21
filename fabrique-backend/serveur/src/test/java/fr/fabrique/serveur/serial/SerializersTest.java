package fr.fabrique.serveur.serial;

import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import fr.fabrique.serveur.model.Commande;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests du format de sérialisation
 */
@DisplayName("Sérialisation maison")
class SerializersTest {

    private static final String ID = UUID.randomUUID().toString();

    @Nested
    @DisplayName("MessageFormat — symétrie encoder/decoder")
    class FormatBasNiveau {

        @Test
        @DisplayName("Message simple → encodé puis décodé = original")
        void allerRetourSimple() throws Exception {
            String payload = MessageFormat.encoder("ORDER", "id", "abc", "CLAUDE", "2");
            Message msg = MessageFormat.decoder(payload);
            assertEquals("ORDER", msg.type());
            assertTrue(msg.champs().containsKey("id"));
            assertTrue(msg.champs().containsValue("abc"));
            assertTrue(msg.champs().containsKey("CLAUDE"));
            assertTrue(msg.champs().containsValue("2"));
        }

        @Test
        @DisplayName("Valeur contenant ':' correctement échappée et restorée")
        void valeurAvecDeuxPointsEchappee() throws Exception {
            String payload = MessageFormat.encoder("ERROR", "reason", "erreur : critique");
            Message msg = MessageFormat.decoder(payload);
            assertEquals("erreur : critique",msg.champObligatoire("reason"));
        }

        @Test
        @DisplayName("Valeur contenant '|' correctement échappée et restorée")
        void valeurAvecPipeEchappee() throws Exception {
            String payload = MessageFormat.encoder("ERROR", "reason", "msg|probleme");
            Message msg = MessageFormat.decoder(payload);
            assertEquals("msg|probleme", msg.champObligatoire("reason"));
        }

        @Test
        @DisplayName("Payload vide renvoie SerialException")
        void payloadVideException() {
            assertThrows(SerialException.class, () -> MessageFormat.decoder(""));
        }

        @Test
        @DisplayName("Payload null renvoie SerialException")
        void payloadNullException() {
            assertThrows(SerialException.class, () -> MessageFormat.decoder(null));
        }

        @Test
        @DisplayName("Paire sans ':' renvoie SerialException")
        void paireSansColonException() {
            assertThrows(SerialException.class, () -> MessageFormat.decoder("ORDER|idABC"));
        }
    }

    @Nested
    @DisplayName("Commande ORDER")
    class CommandeTest {

        @Test
        @DisplayName("Aller-retour Commande -> payload -> Commande")
        void allerRetourCommande() throws Exception {
            Commande avant = new Commande(ID,
                    Map.of(TypeLunette.CLAUDE, 3, TypeLunette.BANANA, 1));

            String payload = Serializers.encoderCommande(avant);
            Commande apres = Serializers.decoderCommande(payload);

            assertEquals(ID, apres.orderId());
            assertTrue(apres.typesQuantites().containsKey(TypeLunette.CLAUDE));
            assertEquals(3, apres.typesQuantites().get(TypeLunette.CLAUDE));
            assertTrue(apres.typesQuantites().containsKey(TypeLunette.BANANA));
            assertEquals(1, apres.typesQuantites().get(TypeLunette.BANANA));
        }

        @Test
        @DisplayName("Type inconnu dans payload ORDER -> SerialException")
        void typeInconnuException() {
            String payload = "ORDER|id:" + ID + "|FOOBAR:2";
            Throwable exception = assertThrows(SerialException.class, () -> Serializers.decoderCommande(payload));
            assertTrue(exception.getMessage().contains("FOOBAR"));
        }

        @Test
        @DisplayName("Quantité non entière renvoie SerialException")
        void qteNonEntiereException() {
            String payload = "ORDER|id:" + ID + "|CLAUDE:deux";
            Throwable exception = assertThrows(SerialException.class, () -> Serializers.decoderCommande(payload));
            assertTrue(exception.getMessage().contains("CLAUDE"));
        }
    }

    @Nested
    @DisplayName("Livraison DELIVERY")
    class LivraisonTest {

        @Test
        @DisplayName("Aller-retour livraison")
        void allerRetourLivraison() throws Exception {
            List<Lunette> lunettes = List.of(
                    new Lunette(TypeLunette.CLAUDE, "AB-123456-789012"),
                    new Lunette(TypeLunette.BANANA, "CD-654321-210987"));

            String payload = Serializers.encoderDelivery(ID, lunettes);
            Map<String, TypeLunette> result = Serializers.decoderDelivery(payload);

            assertTrue(result.containsKey("AB-123456-789012"));
            assertEquals(TypeLunette.CLAUDE, result.get("AB-123456-789012"));
            assertTrue(result.containsKey("CD-654321-210987"));
            assertEquals(TypeLunette.BANANA, result.get("CD-654321-210987"));
        }
    }

    @Nested
    @DisplayName("Vérification de numéro de série")
    class SerialTest {

        @Test
        @DisplayName("Numéro valide -> type dans le payload")
        void serialValideEncodeType() throws Exception {
            String payload = Serializers.encoderSerialResult("AB-123456-789012", TypeLunette.CLAUDE);
            Message msg = MessageFormat.decoder(payload);
            assertEquals("CLAUDE", msg.champObligatoire("type"));
        }

        @Test
        @DisplayName("Numéro invalide (type null) -> 'invalid' dans le payload")
        void serialInvalideEncodeInvalid() throws Exception {
            String payload = Serializers.encoderSerialResult("XX-000000-000000", null);
            Message msg = MessageFormat.decoder(payload);
            assertEquals("invalid", msg.champObligatoire("type"));
        }
    }

    @Nested
    @DisplayName("Messages simples")
    class MessagesSimples {

        @Test
        @DisplayName("VALIDATED contient l'orderId")
        void validatedContientId() throws Exception {
            String payload = Serializers.encoderValidated(ID);
            Message msg = MessageFormat.decoder(payload);
            assertEquals("VALIDATED", msg.type());
            assertEquals(ID, msg.champObligatoire("id"));
        }

        @Test
        @DisplayName("CANCELLED contient l'orderId et le motif")
        void cancelledContientIdEtReason() throws Exception {
            String payload = Serializers.encoderCancelled(ID, "type inconnu");
            assertEquals("type inconnu", Serializers.decoderCancelledReason(payload));
        }

        @Test
        @DisplayName("STATUS contient processing")
        void statusProcessing() throws Exception {
            String payload = Serializers.encoderStatus(ID, "processing");
            Message msg = MessageFormat.decoder(payload);
            assertEquals("processing", msg.champObligatoire("status"));
        }
    }
}
