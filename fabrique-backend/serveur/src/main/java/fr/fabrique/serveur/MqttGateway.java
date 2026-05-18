package fr.fabrique.serveur;

import fr.fabrique.usine.Usine;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Passerelle MQTT du backend.
 */
public class MqttGateway {

    private static final Logger LOG = LoggerFactory.getLogger(MqttGateway.class);

    private final Config config;
    private final Usine usine;
    private MqttClient client;

    public MqttGateway(Config config, Usine usine) {
        this.config = config;
        this.usine = usine;
    }

    public void start() {
        LOG.info("Démarrage de la passerelle MQTT vers {}", config.brokerUrl());
        throw new UnsupportedOperationException("À implémenter");
    }

    public void stop() {
        LOG.info("Arrêt de la passerelle MQTT");
    }
}
