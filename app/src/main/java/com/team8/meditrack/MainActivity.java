package com.team8.meditrack;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MqttHandler mqttHandler;
    private String brokerIp = "192.168.113.182"; // Default IP, should be configurable

    // UI elements
    private TextView tempTextView;
    private TextView heartRateTextView;
    private TextView spo2TextView;
    private TextView deviceStatusTextView;
    private TextView locationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make status bar transparent
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // Make content display behind status bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_main);

        // Initialize UI elements
        tempTextView = findViewById(R.id.textViewTemperature);
        heartRateTextView = findViewById(R.id.textViewHeartRate);
        spo2TextView = findViewById(R.id.textViewSpO2);
        deviceStatusTextView = findViewById(R.id.textViewDeviceStatus);
        locationTextView = findViewById(R.id.textViewLocation);

        // Set up button click listeners
        findViewById(R.id.buttonSetMedication).setOnClickListener(v -> {
            // Show a dialog to enter medication details
            showMedicationDialog();
        });

        findViewById(R.id.buttonSetThresholds).setOnClickListener(v -> {
            // Show a dialog to configure thresholds
            showThresholdsDialog();
        });

        findViewById(R.id.buttonTestConnection).setOnClickListener(v -> {
            // Show a dialog to enter broker IP
            showBrokerIpDialog();
        });

        // Auto-connect to broker when app starts
        new android.os.Handler().postDelayed(() -> {
            Log.d(TAG, "Auto-connecting to MQTT broker at " + brokerIp);
            Toast.makeText(MainActivity.this, "Auto-connecting to broker...", Toast.LENGTH_SHORT).show();
            startMqttConnection();
        }, 1000);
    }

    private void startMqttConnection() {
        try {
            // Define topics to subscribe to
            String[] topics = {
                    "meditrack/temperature",
                    "meditrack/heartrate",
                    "meditrack/spo2",
                    "meditrack/device_status",
                    "meditrack/medication_confirm",
                    "meditrack/location"
            };

            // Create a unique client ID using device ID or a random UUID
            String clientId = "MediTrackApp-" + System.currentTimeMillis();

            Log.d(TAG, "Starting MQTT connection to " + brokerIp + " with client ID " + clientId);

            // Initialize MQTT handler with callback for message handling
            mqttHandler = new MqttHandler(
                    getApplicationContext(),
                    brokerIp,
                    clientId,
                    new MqttCallbackExtended() {
                        @Override
                        public void connectComplete(boolean reconnect, String serverURI) {
                            if (reconnect) {
                                Log.d(TAG, "Reconnected to MQTT broker at " + serverURI);
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "Reconnected to MQTT broker", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                Log.d(TAG, "Connected to MQTT broker at " + serverURI);
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "Connected to MQTT broker", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }

                        @Override
                        public void connectionLost(Throwable cause) {
                            Log.e(TAG, "Connection to MQTT broker lost", cause);
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Connection to MQTT broker lost", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            String payload = new String(message.getPayload());
                            Log.d(TAG, "Message received: " + topic + " - " + payload);

                            // Show a toast notification when a message arrives (for debugging)
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,
                                        "Message on: " + topic,
                                        Toast.LENGTH_SHORT).show();
                            });

                            // Process the message based on the topic
                            processMessage(topic, payload);
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {
                            Log.d(TAG, "Message delivery complete");
                        }
                    },
                    topics
            );
        } catch (Exception e) {
            Log.e(TAG, "Error starting MQTT connection", e);
            Toast.makeText(this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Checks if a string is valid JSON
     * @param str String to check
     * @return true if valid JSON, false otherwise
     */
    private boolean isValidJson(String str) {
        try {
            new JSONObject(str);
            return true;
        } catch (JSONException ex) {
            try {
                new org.json.JSONArray(str);
                return true;
            } catch (JSONException ex1) {
                return false;
            }
        }
    }
    private void processMessage(String topic, String payload) {
        Log.d(TAG, "Processing message on topic: " + topic + " with payload: " + payload);

        try {
            // Handle different message types based on topic
            switch (topic) {
                case "meditrack/temperature":
                    updateTemperature(payload);
                    break;
                case "meditrack/heartrate":
                    updateHeartRate(payload);
                    break;
                case "meditrack/spo2":
                    updateSpO2(payload);
                    break;
                case "meditrack/device_status":
                    updateDeviceStatus(payload);
                    break;
                case "meditrack/medication_confirm":
                    handleMedicationConfirmation(payload);
                    break;
                case "meditrack/location":
                    updateLocation(payload);
                    break;
                default:
                    Log.d(TAG, "Received message on unhandled topic: " + topic);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing message: " + e.getMessage(), e);
        }
    }

    private void showBrokerIpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("MQTT Broker Configuration");

        // Inflate the dialog layout
        View view = getLayoutInflater().inflate(R.layout.dialog_broker_config, null);
        final EditText ipEditText = view.findViewById(R.id.editTextBrokerIp);
        ipEditText.setText(brokerIp);

        builder.setView(view);

        builder.setPositiveButton("Connect", (dialog, which) -> {
            brokerIp = ipEditText.getText().toString().trim();

            // Disconnect existing connection if any
            if (mqttHandler != null) {
                mqttHandler.disconnect();
            }

            // Start new connection
            startMqttConnection();
            Toast.makeText(MainActivity.this, "Connecting to broker at " + brokerIp, Toast.LENGTH_SHORT).show();

            // Add a slight delay to ensure connection is established before sending test message
            new android.os.Handler().postDelayed(() -> {
                if (mqttHandler != null && mqttHandler.isConnected()) {
                    // Publish a test message
                    publishTestMessage();
                }
            }, 2000); // 2-second delay
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void publishTestMessage() {
        try {
            if (mqttHandler == null || !mqttHandler.isConnected()) {
                Toast.makeText(MainActivity.this, "MQTT not connected. Cannot publish test message.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create test messages for different topics to test UI updates
            publishTestTemperature();

            // Add a delay between messages
            new android.os.Handler().postDelayed(this::publishTestHeartRate, 1000);

            // Add another delay for the next message
            new android.os.Handler().postDelayed(this::publishTestSpO2, 2000);

            // Add another delay for device status
            new android.os.Handler().postDelayed(this::publishTestDeviceStatus, 3000);
        } catch (Exception e) {
            Log.e(TAG, "Error in test message sequence", e);
            Toast.makeText(MainActivity.this, "Error in test sequence: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void publishTestTemperature() {
        // Create a simple test message for temperature
        String testMessage = "{\"value\": 36.8}";
        mqttHandler.publishMessage("meditrack/temperature", testMessage);
        Toast.makeText(MainActivity.this, "Test temperature published", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Test message published to meditrack/temperature: " + testMessage);
    }

    private void publishTestHeartRate() {
        // Create a simple test message for heart rate
        String testMessage = "{\"value\": 75}";
        mqttHandler.publishMessage("meditrack/heartrate", testMessage);
        Toast.makeText(MainActivity.this, "Test heart rate published", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Test message published to meditrack/heartrate: " + testMessage);
    }

    private void publishTestSpO2() {
        // Create a simple test message for SpO2
        String testMessage = "{\"value\": 98}";
        mqttHandler.publishMessage("meditrack/spo2", testMessage);
        Toast.makeText(MainActivity.this, "Test SpO2 published", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Test message published to meditrack/spo2: " + testMessage);
    }

    private void publishTestDeviceStatus() {
        // Create a simple test message for device status
        String testMessage = "{\"worn\": true}";
        mqttHandler.publishMessage("meditrack/device_status", testMessage);
        Toast.makeText(MainActivity.this, "Test device status published", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Test message published to meditrack/device_status: " + testMessage);
    }

    private void showMedicationDialog() {
        if (mqttHandler == null || !mqttHandler.isConnected()) {
            Toast.makeText(this, "Please connect to MQTT broker first", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Schedule Medication");

        // Inflate the dialog layout
        View view = getLayoutInflater().inflate(R.layout.dialog_medication, null);
        final EditText medicationEditText = view.findViewById(R.id.editTextMedication);
        final EditText timeEditText = view.findViewById(R.id.editTextTime);

        builder.setView(view);

        builder.setPositiveButton("Schedule", (dialog, which) -> {
            String medication = medicationEditText.getText().toString().trim();
            String time = timeEditText.getText().toString().trim();

            if (!medication.isEmpty() && !time.isEmpty()) {
                mqttHandler.scheduleMedication(medication, time);
                Toast.makeText(MainActivity.this, "Medication scheduled: " + medication + " at " + time, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Please enter both medication name and time", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showThresholdsDialog() {
        if (mqttHandler == null || !mqttHandler.isConnected()) {
            Toast.makeText(this, "Please connect to MQTT broker first", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Alert Thresholds");

        // Inflate the dialog layout
        View view = getLayoutInflater().inflate(R.layout.dialog_thresholds, null);
        final EditText minTempEditText = view.findViewById(R.id.editTextMinTemp);
        final EditText maxTempEditText = view.findViewById(R.id.editTextMaxTemp);
        final EditText minHrEditText = view.findViewById(R.id.editTextMinHeartRate);
        final EditText maxHrEditText = view.findViewById(R.id.editTextMaxHeartRate);
        final EditText minSpo2EditText = view.findViewById(R.id.editTextMinSpO2);

        // Set default values
        minTempEditText.setText("35.0");
        maxTempEditText.setText("38.0");
        minHrEditText.setText("60");
        maxHrEditText.setText("100");
        minSpo2EditText.setText("90");

        builder.setView(view);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                double minTemp = Double.parseDouble(minTempEditText.getText().toString());
                double maxTemp = Double.parseDouble(maxTempEditText.getText().toString());
                int minHr = Integer.parseInt(minHrEditText.getText().toString());
                int maxHr = Integer.parseInt(maxHrEditText.getText().toString());
                int minSpo2 = Integer.parseInt(minSpo2EditText.getText().toString());

                mqttHandler.setAlertThresholds(minTemp, maxTemp, minHr, maxHr, minSpo2);
                Toast.makeText(MainActivity.this, "Alert thresholds updated", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Methods to update UI with received data
    private void updateTemperature(String payload) {
        try {
            Log.d(TAG, "updateTemperature called with payload: " + payload);

            // Check if payload is valid JSON
            if (!isValidJson(payload)) {
                Log.w(TAG, "Received non-JSON payload for temperature: " + payload);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Try to parse the payload as a number directly
                            try {
                                double temp = Double.parseDouble(payload.trim());
                                tempTextView.setText(String.format("Temperature: %.1f°C", temp));

                                // Check for abnormal values (using default thresholds)
                                if (temp < 35.0 || temp > 38.0) {
                                    tempTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                } else {
                                    tempTextView.setTextColor(getResources().getColor(android.R.color.black));
                                }
                                Log.d(TAG, "Temperature TextView updated with direct number");
                            } catch (NumberFormatException nfe) {
                                // If not a number, just display as text
                                tempTextView.setText("Temperature: " + payload);
                                tempTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                                Log.d(TAG, "Temperature TextView updated with non-JSON text");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating temperature TextView with text", e);
                        }
                    }
                });
                return;
            }

            // Process as JSON
            JSONObject json = new JSONObject(payload);
            final double temperature = json.getDouble("value");

            Log.d(TAG, "Temperature parsed: " + temperature);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Updating temperature TextView to: " + temperature);
                        // Update temperature text
                        tempTextView.setText(String.format("Temperature: %.1f°C", temperature));

                        // Check for abnormal values (using default thresholds)
                        if (temperature < 35.0 || temperature > 38.0) {
                            tempTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        } else {
                            tempTextView.setTextColor(getResources().getColor(android.R.color.black));
                        }

                        Log.d(TAG, "Temperature TextView updated successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating temperature TextView", e);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing temperature data: " + e.getMessage() + ", payload: " + payload, e);

            // Handle error by showing a warning in the UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        tempTextView.setText("Temp: Invalid format");
                        tempTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        Log.d(TAG, "Temperature updated to show error");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating temperature for error condition", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateTemperature", e);
        }
    }

    private void updateHeartRate(String payload) {
        try {
            Log.d(TAG, "updateHeartRate called with payload: " + payload);

            // Check if payload is valid JSON
            if (!isValidJson(payload)) {
                Log.w(TAG, "Received non-JSON payload for heart rate: " + payload);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Try to parse the payload as a number directly
                            try {
                                int hr = Integer.parseInt(payload.trim());
                                heartRateTextView.setText(String.format("Heart Rate: %d BPM", hr));

                                // Check for abnormal values (using default thresholds)
                                if (hr < 60 || hr > 100) {
                                    heartRateTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                } else {
                                    heartRateTextView.setTextColor(getResources().getColor(android.R.color.black));
                                }
                                Log.d(TAG, "Heart rate TextView updated with direct number");
                            } catch (NumberFormatException nfe) {
                                // If not a number, just display as text
                                heartRateTextView.setText("Heart Rate: " + payload);
                                heartRateTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                                Log.d(TAG, "Heart rate TextView updated with non-JSON text");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating heart rate TextView with text", e);
                        }
                    }
                });
                return;
            }

            // Process as JSON
            JSONObject json = new JSONObject(payload);
            final int heartRate = json.getInt("value");

            Log.d(TAG, "Heart rate parsed: " + heartRate);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Updating heart rate TextView to: " + heartRate);
                        // Update heart rate text
                        heartRateTextView.setText(String.format("Heart Rate: %d BPM", heartRate));

                        // Check for abnormal values (using default thresholds)
                        if (heartRate < 60 || heartRate > 100) {
                            heartRateTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        } else {
                            heartRateTextView.setTextColor(getResources().getColor(android.R.color.black));
                        }

                        Log.d(TAG, "Heart rate TextView updated successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating heart rate TextView", e);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing heart rate data: " + e.getMessage() + ", payload: " + payload, e);

            // Handle error by showing a warning in the UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        heartRateTextView.setText("HR: Invalid format");
                        heartRateTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        Log.d(TAG, "Heart rate updated to show error");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating heart rate for error condition", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateHeartRate", e);
        }
    }

    // Replace the existing updateSpO2 method with this updated version
    private void updateSpO2(String payload) {
        try {
            Log.d(TAG, "updateSpO2 called with payload: " + payload);

            // Check if payload is valid JSON
            if (!isValidJson(payload)) {
                Log.w(TAG, "Received non-JSON payload for SpO2: " + payload);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Try to parse the payload as a number directly
                            try {
                                int spo2 = Integer.parseInt(payload.trim());
                                spo2TextView.setText(String.format("SpO2: %d%%", spo2));

                                // Check for abnormal values (using default threshold)
                                if (spo2 < 90) {
                                    spo2TextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                } else {
                                    spo2TextView.setTextColor(getResources().getColor(android.R.color.black));
                                }
                                Log.d(TAG, "SpO2 TextView updated with direct number");
                            } catch (NumberFormatException nfe) {
                                // If not a number, just display as text
                                spo2TextView.setText("SpO2: " + payload);
                                spo2TextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                                Log.d(TAG, "SpO2 TextView updated with non-JSON text");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating SpO2 TextView with text", e);
                        }
                    }
                });
                return;
            }

            // Process as JSON
            JSONObject json = new JSONObject(payload);
            final int spo2 = json.getInt("value");

            Log.d(TAG, "SpO2 parsed: " + spo2);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Updating SpO2 TextView to: " + spo2);
                        // Update SpO2 text
                        spo2TextView.setText(String.format("SpO2: %d%%", spo2));

                        // Check for abnormal values (using default threshold)
                        if (spo2 < 90) {
                            spo2TextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        } else {
                            spo2TextView.setTextColor(getResources().getColor(android.R.color.black));
                        }

                        Log.d(TAG, "SpO2 TextView updated successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating SpO2 TextView", e);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing SpO2 data: " + e.getMessage() + ", payload: " + payload, e);

            // Handle error by showing a warning in the UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        spo2TextView.setText("SpO2: Invalid format");
                        spo2TextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        Log.d(TAG, "SpO2 updated to show error");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating SpO2 for error condition", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateSpO2", e);
        }
    }

    private void updateDeviceStatus(String payload) {
        try {
            Log.d(TAG, "updateDeviceStatus called with payload: " + payload);

            // Check if payload is valid JSON
            if (!isValidJson(payload)) {
                Log.w(TAG, "Received non-JSON payload for device status: " + payload);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Handle as plain text for debugging purposes
                            deviceStatusTextView.setText("Device Status: " + payload);
                            deviceStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                            Log.d(TAG, "Device status TextView updated with non-JSON text");
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating device status TextView with text", e);
                        }
                    }
                });
                return;
            }

            // Process as JSON
            JSONObject json = new JSONObject(payload);
            final boolean isWorn = json.getBoolean("worn");

            Log.d(TAG, "Device status parsed: " + (isWorn ? "worn" : "not worn"));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Updating device status TextView to: " + (isWorn ? "worn" : "not worn"));
                        if (isWorn) {
                            deviceStatusTextView.setText("Device Status: Worn");
                            deviceStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        } else {
                            deviceStatusTextView.setText("Device Status: Not Worn");
                            deviceStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        }

                        Log.d(TAG, "Device status TextView updated successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating device status TextView", e);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing device status data: " + e.getMessage() + ", payload: " + payload, e);

            // Handle error by showing a warning in the UI
            final String errorMessage = payload;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        deviceStatusTextView.setText("Status: Invalid format");
                        deviceStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        Log.d(TAG, "Device status updated to show error");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating device status for error condition", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateDeviceStatus", e);
        }
    }

    private void handleMedicationConfirmation(String payload) {
        try {
            Log.d(TAG, "handleMedicationConfirmation called with payload: " + payload);
            JSONObject json = new JSONObject(payload);
            final String medicationName = json.getString("medication");
            final boolean taken = json.getBoolean("taken");

            Log.d(TAG, "Medication confirmation parsed: " + medicationName + " " + (taken ? "taken" : "missed"));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = medicationName + (taken ? " taken" : " missed");
                        Log.d(TAG, "Showing medication toast: " + message);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing medication toast", e);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing medication confirmation: " + e.getMessage() + ", payload: " + payload, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in handleMedicationConfirmation", e);
        }
    }

    private void updateLocation(String payload) {
        try {
            Log.d(TAG, "updateLocation called with payload: " + payload);
            JSONObject json = new JSONObject(payload);
            final double latitude = json.getDouble("lat");
            final double longitude = json.getDouble("lng");

            Log.d(TAG, "Location parsed: " + latitude + ", " + longitude);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Updating location TextView to: " + latitude + ", " + longitude);
                        locationTextView.setText(String.format("Location: %.6f, %.6f", latitude, longitude));
                        Log.d(TAG, "Location TextView updated successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating location TextView", e);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing location data: " + e.getMessage() + ", payload: " + payload, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateLocation", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttHandler != null) {
            mqttHandler.disconnect();
        }
    }
}