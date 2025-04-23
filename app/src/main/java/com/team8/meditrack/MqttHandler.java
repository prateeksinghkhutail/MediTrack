package com.team8.meditrack;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * A simplified MQTT handler that uses the regular MQTT client instead of the Android-specific one
 * to avoid compatibility issues with AndroidX and the Support Library
 */
public class MqttHandler {
    private static final String TAG = "MqttHandler";

    private final String serverUri;
    private final String clientId;
    private final String[] subscriptionTopics;

    private MqttClient mqttClient;
    private final MqttCallbackExtended callback;
    private final Context context;

    public MqttHandler(Context context, String serverIp, String clientId,
                       MqttCallbackExtended callback, String[] topics) {
        this.serverUri = "tcp://" + serverIp + ":1883";
        this.clientId = clientId;
        this.callback = callback;
        this.subscriptionTopics = topics;
        this.context = context;

        // Initialize and connect
        connect();
    }

    private void connect() {
        try {
            // Use MemoryPersistence to avoid file issues
            MemoryPersistence persistence = new MemoryPersistence();

            Log.d(TAG, "Initializing MQTT client with server URI: " + serverUri + ", client ID: " + clientId);

            // Create MQTT client
            mqttClient = new MqttClient(serverUri, clientId, persistence);

            // Set up callback adapter to convert between MqttCallback and MqttCallbackExtended
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection lost to broker: " + serverUri, cause);
                    callback.connectionLost(cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Message arrived - Topic: " + topic + ", Message: " + payload);
                    callback.messageArrived(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Message delivery complete");
                    callback.deliveryComplete(token);
                }
            });

            // Configure connection options
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(false);
            mqttConnectOptions.setConnectionTimeout(30); // Increase timeout to 30 seconds

            Log.d(TAG, "Connecting to MQTT broker...");

            // Connect and handle connection completion
            mqttClient.connect(mqttConnectOptions);

            // Log successful connection
            Log.d(TAG, "Successfully connected to MQTT broker: " + serverUri);

            // Notify that connection is complete (since we're not using the async connect)
            callback.connectComplete(false, serverUri);

            // Subscribe to topics after connection
            subscribeToTopics();

        } catch (MqttException ex) {
            Log.e(TAG, "Error connecting to MQTT broker: " + ex.getMessage() + " (Error code: " + ex.getReasonCode() + ")", ex);
        } catch (Exception ex) {
            Log.e(TAG, "Unexpected error during MQTT connection", ex);
        }
    }

    private void subscribeToTopics() {
        if (subscriptionTopics == null || subscriptionTopics.length == 0) {
            Log.w(TAG, "No topics provided for subscription");
            return;
        }

        Log.d(TAG, "Starting to subscribe to " + subscriptionTopics.length + " topics");

        for (String topic : subscriptionTopics) {
            try {
                mqttClient.subscribe(topic, 0);
                Log.d(TAG, "Successfully subscribed to topic: " + topic);
            } catch (MqttException ex) {
                Log.e(TAG, "Error subscribing to topic: " + topic + ", reason code: " + ex.getReasonCode(), ex);
            }
        }

        Log.d(TAG, "Topic subscription process completed");
    }

    public void publishMessage(String topic, String payload) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            Log.e(TAG, "MQTT client not connected, cannot publish message");
            return;
        }

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);

            mqttClient.publish(topic, message);
            Log.d(TAG, "Message published to topic: " + topic);
        } catch (MqttException e) {
            Log.e(TAG, "Error publishing message to topic: " + topic, e);
        }
    }

    // Helper methods to send specific data types
    public void scheduleMedication(String medication, String time) {
        String payload = "{\"medication\":\"" + medication + "\",\"time\":\"" + time + "\"}";
        publishMessage("meditrack/schedule_medication", payload);
    }

    public void setAlertThresholds(double minTemp, double maxTemp, int minHeartRate, int maxHeartRate, int minSpO2) {
        String payload = "{\"minTemp\":" + minTemp + "," +
                "\"maxTemp\":" + maxTemp + "," +
                "\"minHeartRate\":" + minHeartRate + "," +
                "\"maxHeartRate\":" + maxHeartRate + "," +
                "\"minSpO2\":" + minSpO2 + "}";
        publishMessage("meditrack/alert_thresholds", payload);
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                Log.d(TAG, "Disconnected from MQTT broker");
            } catch (MqttException e) {
                Log.e(TAG, "Error disconnecting from MQTT broker", e);
            }
        }
    }
}