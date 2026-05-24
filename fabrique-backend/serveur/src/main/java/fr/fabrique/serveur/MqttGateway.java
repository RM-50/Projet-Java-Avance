package fr.fabrique.serveur;

import fr.fabrique.usine.Usine;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Passerelle MQTT du backend.
 */
public class MqttGateway implements MqttCallback{

    private static final Logger LOG = LoggerFactory.getLogger(MqttGateway.class);

    private static final String TOPIC_ORDERS  = "orders/+";
    private static final String TOPIC_SERIALS = "serials/+/check";

    private final Config config;
    private final Usine usine;
    private MqttClient client;

    private final ExecutorService pool = Executors.newCachedThreadPool();

    public MqttGateway(Config config, Usine usine) {
        this.config = config;
        this.usine = usine;
    }

    /**
     * Méthode start qui sert à démarrer le client MQTT
     * @throws MqttException
     */
    public void start() throws MqttException {
        String clientId = config.clientIdPrefix() + "-" + UUID.randomUUID();
        LOG.info("Connexion au broker {} avec clientId={}", config.brokerUrl(), clientId);

        // Initialisation du client MQTT
        client = new MqttClient(config.brokerUrl(), clientId, new MemoryPersistence());
        client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(20);

        // Connexion au broker MQTT
        client.connect(options);
        LOG.info("Connecte au broker MQTT");

        client.subscribe(TOPIC_ORDERS,  config.qos());
        client.subscribe(TOPIC_SERIALS, config.qos());
        LOG.info("Abonnements actifs : '{}', '{}'", TOPIC_ORDERS, TOPIC_SERIALS);
    }

    public void stop() {
        LOG.info("Arret de la passerelle MQTT...");
        pool.shutdown();
        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                LOG.warn("Certains traitements n'ont pas termine dans le delai imparti");
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }

        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                LOG.info("Deconnecte du broker MQTT");
            } catch (MqttException e) {
                LOG.warn("Erreur lors de la deconnexion", e);
            }
        }
    }

    /**
     * Méthode publier permet de publier un payload sur un topic
     * @param topic topic sur lequel on souhaite publier
     * @param payload payload que l'on souhaite envoyer
     */
    public void publier(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(config.qos());
            message.setRetained(false);
            client.publish(topic, message);
            LOG.debug("Publie sur '{}' : {}", topic, payload);
        } catch (MqttException e) {
            LOG.error("Erreur de publication sur '{}'", topic, e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        byte[] payload = message.getPayload();
        LOG.debug("Message reçu sur '{}' ({} octets)", topic, payload.length);

        if (matcheOrders(topic)) {
            // Extrait l'orderId depuis "orders/xxx"
            String orderId = topic.split("/")[1];
            pool.submit(() -> {
                OrderHandler handler = new OrderHandler(orderId, payload, usine, this);
                handler.handle();
            });

        } else if (matcheSerials(topic)) {
            // Extrait le serial depuis "serials/xxx/check"
            String serial = topic.split("/")[1];
            pool.submit(() -> {
                SerialChecker checker = new SerialChecker(serial, this);
                checker.handle();
            });

        } else {
            LOG.warn("Message reçu sur topic non gere : '{}'", topic);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        LOG.warn("Connexion perdue : {} — reconnexion automatique en cours", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Non utilisé côté abonné
    }

    /**
     * Méthode matcheOrders qui vérifie que le topic est de la forme orders/{uuid}
     */
    private boolean matcheOrders(String topic) {
        String[] parts = topic.split("/");
        return parts.length == 2 && "orders".equals(parts[0]);
    }

    /** Méthode matcheSerials qui vérifie que le topic est de la forme {@code serials/<serial>/check}. */
    private boolean matcheSerials(String topic) {
        String[] parts = topic.split("/");
        return parts.length == 3 && "serials".equals(parts[0]) && "check".equals(parts[2]);
    }
}
