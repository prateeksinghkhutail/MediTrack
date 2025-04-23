package com.team8.meditrack;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * A simple adapter class that converts MqttCallbackExtended to MqttCallback
 * This allows us to maintain compatibility with code that expects the Extended version
 */
public class MqttCallbackAdapter implements MqttCallback {

    private final MqttCallbackExtended callback;

    public MqttCallbackAdapter(MqttCallbackExtended callback) {
        this.callback = callback;
    }

    @Override
    public void connectionLost(Throwable cause) {
        callback.connectionLost(cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        callback.messageArrived(topic, message);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        callback.deliveryComplete(token);
    }
}