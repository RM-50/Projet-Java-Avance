package fr.fabrique.frontend.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Client MQTT du frontend.
 */
public class MqttFrontendClient implements MqttCallback {

    private static final Logger LOG = LoggerFactory.getLogger(MqttFrontendClient.class);
    private static final String CONFIG_RESOURCE = "config.properties";

    private static MqttFrontendClient instance;

    private MqttClient client;
    private int qos = 1;

    private BiConsumer<String, String> messageCallback;

    private MqttFrontendClient() {}

    public static MqttFrontendClient getInstance() {
        if (instance == null) {
            instance = new MqttFrontendClient();
        }
        return instance;
    }

    /**
     * Connecte au broker. Appelé une seule fois au démarrage de l'application.
     *
     * @throws MqttException si la connexion échoue
     */
    public void connecter() throws MqttException {
        Properties props = chargerConfig();
        String brokerUrl  = props.getProperty("mqtt.broker.url", "tcp://localhost:1883");
        String idPrefix   = props.getProperty("mqtt.client.id.prefix", "fabrique-frontend");
        qos = Integer.parseInt(props.getProperty("mqtt.qos", "1"));

        String clientId = idPrefix + "-" + UUID.randomUUID();
        LOG.info("Connexion MQTT : broker={}, clientId={}", brokerUrl, clientId);

        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);

        client.connect(options);
        LOG.info("Frontend connecté au broker MQTT");
    }

    public void deconnecter() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                LOG.info("Frontend déconnecté du broker MQTT");
            } catch (MqttException e) {
                LOG.warn("Erreur déconnexion", e);
            }
        }
    }


    /**
     * Méthode publier qui permet d'envoyer un payload sur un topic
     * @param topic
     * @param payload
     */
    public void publier(String topic, String payload) {
        try {
            MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            msg.setQos(qos);
            client.publish(topic, msg);
            LOG.debug("Publié sur '{}' : {}", topic, payload);
        } catch (MqttException e) {
            LOG.error("Erreur publication sur '{}'", topic, e);
        }
    }

    /**
     * Méthode abonner qui permet de s'abonner à un topic
     * @param topic
     */
    public void abonner(String topic) {
        try {
            client.subscribe(topic, qos);
            LOG.debug("Abonné à '{}'", topic);
        } catch (MqttException e) {
            LOG.error("Erreur abonnement sur '{}'", topic, e);
        }
    }

    /**
     * Méthode se désabonner qui permet de se désabonner d'un topic
     * @param topic
     */
    public void desabonner(String topic) {
        try {
            client.unsubscribe(topic);
            LOG.debug("Désabonné de '{}'", topic);
        } catch (MqttException e) {
            LOG.warn("Erreur désabonnement de '{}'", topic, e);
        }
    }

    /**
     * Positionne le callback global qui reçoit tous les messages.
     * Le contrôleur actif se branche ici et filtre par topic.
     */
    public void setMessageCallback(BiConsumer<String, String> callback) {
        this.messageCallback = callback;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        LOG.debug("Message reçu sur '{}' : {}", topic, payload);
        if (messageCallback != null) {
            messageCallback.accept(topic, payload);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        LOG.warn("Connexion MQTT perdue : {} — reconnexion auto en cours", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { /* non utilisé */ }

    private Properties chargerConfig() {
        Properties props = new Properties();
        if (Files.exists(Path.of(CONFIG_RESOURCE))) {
            try (InputStream in = Files.newInputStream(Path.of(CONFIG_RESOURCE))) {
                props.load(in);
                return props;
            } catch (IOException e) {
                LOG.warn("Impossible de lire config.properties externe, utilisation de la ressource embarquée");
            }
        }
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            LOG.warn("Impossible de lire la config embarquée", e);
        }
        return props;
    }
}
