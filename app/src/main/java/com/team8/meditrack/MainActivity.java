package com.team8.meditrack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, MedicationAdapter.OnMedicationClickListener {

    private static final String TAG = "MainActivity";

    // MQTT
    private MqttHandler mqttHandler;
    private String brokerIp = "192.168.113.182"; // Default IP

    // UI elements - Health parameters
    private TextView tempTextView;
    private TextView heartRateTextView;
    private TextView spo2TextView;
    private TextView deviceStatusTextView;
    private TextView locationTextView;
    private TextView tempStatusTextView;
    private TextView heartRateStatusTextView;
    private TextView spo2StatusTextView;

    // UI elements - Cards
    private CardView temperatureCard;
    private CardView heartRateCard;
    private CardView spo2Card;

    // UI elements - History buttons
    private Button buttonTempHistory;
    private Button buttonHeartRateHistory;
    private Button buttonSpO2History;

    // UI elements - Medication
    private RecyclerView recyclerViewMedications;
    private TextView textViewNoMedications;
    private Button buttonSetMedication;
    private Button buttonSendAlert;

    // Database
    private AppDatabase database;
    private MedicationAdapter medicationAdapter;

    // Google Maps
    private GoogleMap googleMap;
    private LatLng currentLocation;
    private boolean isMapReady = false;

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

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize UI elements
        initializeUI();

        // Initialize Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Load medications
        loadMedications();

        // Auto-connect to broker when app starts
        new android.os.Handler().postDelayed(() -> {
            Log.d(TAG, "Auto-connecting to MQTT broker at " + brokerIp);
            Toast.makeText(MainActivity.this, "Auto-connecting to broker...", Toast.LENGTH_SHORT).show();
            startMqttConnection();
        }, 1000);
    }

    private void initializeUI() {
        // Initialize health parameter text views
        tempTextView = findViewById(R.id.textViewTemperature);
        heartRateTextView = findViewById(R.id.textViewHeartRate);
        spo2TextView = findViewById(R.id.textViewSpO2);
        deviceStatusTextView = findViewById(R.id.textViewDeviceStatus);
        locationTextView = findViewById(R.id.textViewLocation);
        tempStatusTextView = findViewById(R.id.textViewTemperatureStatus);
        heartRateStatusTextView = findViewById(R.id.textViewHeartRateStatus);
        spo2StatusTextView = findViewById(R.id.textViewSpO2Status);

        // Initialize cards
        temperatureCard = findViewById(R.id.temperatureCard);
        heartRateCard = findViewById(R.id.heartRateCard);
        spo2Card = findViewById(R.id.spo2Card);

        // Initialize history buttons
        buttonTempHistory = findViewById(R.id.buttonTempHistory);
        buttonHeartRateHistory = findViewById(R.id.buttonHeartRateHistory);
        buttonSpO2History = findViewById(R.id.buttonSpO2History);

        // Initialize medication components
        recyclerViewMedications = findViewById(R.id.recyclerViewMedications);
        textViewNoMedications = findViewById(R.id.textViewNoMedications);
        buttonSetMedication = findViewById(R.id.buttonSetMedication);
        buttonSendAlert = findViewById(R.id.buttonSendAlert);

        // Set up RecyclerView
        recyclerViewMedications.setLayoutManager(new LinearLayoutManager(this));
        medicationAdapter = new MedicationAdapter(new ArrayList<>(), this);
        recyclerViewMedications.setAdapter(medicationAdapter);

        // Set up button click listeners
        buttonSetMedication.setOnClickListener(v -> showMedicationDialog());

        findViewById(R.id.buttonTestConnection).setOnClickListener(v -> showBrokerIpDialog());

        buttonSendAlert.setOnClickListener(v -> sendEmergencyAlert());

        // Set up history button click listeners
        buttonTempHistory.setOnClickListener(v -> showParameterHistory("temperature", "Temperature History"));
        buttonHeartRateHistory.setOnClickListener(v -> showParameterHistory("heart_rate", "Heart Rate History"));
        buttonSpO2History.setOnClickListener(v -> showParameterHistory("spo2", "SpO2 History"));
    }

    private void loadMedications() {
        List<Medication> medications = database.medicationDao().getAllMedications();
        medicationAdapter.updateData(medications);

        // Show/hide "no medications" message
        if (medications.isEmpty()) {
            textViewNoMedications.setVisibility(View.VISIBLE);
            recyclerViewMedications.setVisibility(View.GONE);
        } else {
            textViewNoMedications.setVisibility(View.GONE);
            recyclerViewMedications.setVisibility(View.VISIBLE);
        }
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

    // Make sure this is in your processMessage method:
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

            // Add another delay for location
            new android.os.Handler().postDelayed(this::publishTestLocation, 4000);
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

    private void publishTestLocation() {
        // Create a simple test message for location
        String testMessage = "{\"lat\": 17.4431, \"lng\": 78.3496}";  // Example: Hyderabad, India
        mqttHandler.publishMessage("meditrack/location", testMessage);
        Toast.makeText(MainActivity.this, "Test location published", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Test message published to meditrack/location: " + testMessage);
    }

    private void sendEmergencyAlert() {
        if (mqttHandler == null || !mqttHandler.isConnected()) {
            Toast.makeText(this, "Please connect to MQTT broker first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create an emergency alert message
            String alertMessage = "{\"alert\": true, \"message\": \"Emergency alert from caregiver app!\"}";
            mqttHandler.publishMessage("meditrack/emergency_alert", alertMessage);

            Toast.makeText(this, "Emergency alert sent!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Emergency alert sent: " + alertMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error sending emergency alert", e);
            Toast.makeText(this, "Failed to send alert: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
            String medicationName = medicationEditText.getText().toString().trim();
            String time = timeEditText.getText().toString().trim();

            if (!medicationName.isEmpty() && !time.isEmpty()) {
                // Send to MQTT broker
                mqttHandler.scheduleMedication(medicationName, time);

                // Save to local database
                Medication medication = new Medication(medicationName, time, System.currentTimeMillis());
                database.medicationDao().insert(medication);

                // Refresh the medication list
                loadMedications();

                Toast.makeText(MainActivity.this, "Medication scheduled: " + medicationName + " at " + time, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Please enter both medication name and time", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showParameterHistory(String parameterType, String title) {
        // Get the history data from database
        List<HealthParameter> historyData = database.healthParameterDao().getLatestByType(parameterType);

        if (historyData.isEmpty()) {
            Toast.makeText(this, "No history data available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and show dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_parameter_history);

        // Set dialog title
        TextView textViewTitle = dialog.findViewById(R.id.textViewHistoryTitle);
        textViewTitle.setText(title);

        // Set up RecyclerView
        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ParameterHistoryAdapter adapter = new ParameterHistoryAdapter(historyData, parameterType);
        recyclerView.setAdapter(adapter);

        dialog.show();
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
                                tempTextView.setText(String.format("%.1f°C", temp));

                                // Save to database
                                saveHealthParameter("temperature", temp);

                                // Update status and card color
                                updateTemperatureStatus(temp);

                            } catch (NumberFormatException nfe) {
                                // If not a number, just display as text
                                tempTextView.setText(payload);
                                tempStatusTextView.setText("Unknown");
                                tempStatusTextView.setTextColor(Color.BLUE);
                                temperatureCard.setCardBackgroundColor(Color.WHITE);
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

            // Save to database
            saveHealthParameter("temperature", temperature);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Update temperature text
                        tempTextView.setText(String.format("%.1f°C", temperature));

                        // Update status and card color
                        updateTemperatureStatus(temperature);

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
                        tempTextView.setText("Invalid format");
                        tempStatusTextView.setText("Error");
                        tempStatusTextView.setTextColor(Color.RED);
                        temperatureCard.setCardBackgroundColor(Color.parseColor("#FFCCCC"));
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating temperature for error condition", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateTemperature", e);
        }
    }

    private void updateTemperatureStatus(double temperature) {
        // Normal range: 36.1°C to 37.2°C
        if (temperature < 35.0) {
            // Hypothermia
            tempStatusTextView.setText("Low - Hypothermia");
            tempStatusTextView.setTextColor(Color.BLUE);
            temperatureCard.setCardBackgroundColor(Color.parseColor("#E1F5FE"));  // Light blue
        } else if (temperature <= 36.0) {
            // Slightly low
            tempStatusTextView.setText("Slightly Low");
            tempStatusTextView.setTextColor(Color.parseColor("#2196F3"));  // Blue
            temperatureCard.setCardBackgroundColor(Color.parseColor("#E3F2FD"));  // Very light blue
        } else if (temperature <= 37.2) {
            // Normal
            tempStatusTextView.setText("Normal");
            tempStatusTextView.setTextColor(Color.parseColor("#4CAF50"));  // Green
            temperatureCard.setCardBackgroundColor(Color.WHITE);
        } else if (temperature <= 38.0) {
            // Slightly elevated
            tempStatusTextView.setText("Slightly Elevated");
            tempStatusTextView.setTextColor(Color.parseColor("#FF9800"));  // Orange
            temperatureCard.setCardBackgroundColor(Color.parseColor("#FFF3E0"));  // Very light orange
        } else if (temperature <= 39.0) {
            // Fever
            tempStatusTextView.setText("Fever");
            tempStatusTextView.setTextColor(Color.parseColor("#F44336"));  // Red
            temperatureCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"));  // Very light red
        } else {
            // High fever
            tempStatusTextView.setText("High Fever");
            tempStatusTextView.setTextColor(Color.parseColor("#D50000"));  // Dark red
            temperatureCard.setCardBackgroundColor(Color.parseColor("#FFCDD2"));  // Light red
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
                                heartRateTextView.setText(String.format("%d BPM", hr));

                                // Save to database
                                saveHealthParameter("heart_rate", hr);

                                // Update status and card color
                                updateHeartRateStatus(hr);

                            } catch (NumberFormatException nfe) {
                                // If not a number, just display as text
                                heartRateTextView.setText(payload);
                                heartRateStatusTextView.setText("Unknown");
                                heartRateStatusTextView.setTextColor(Color.BLUE);
                                heartRateCard.setCardBackgroundColor(Color.WHITE);
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

            // Save to database
            saveHealthParameter("heart_rate", heartRate);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Update heart rate text
                        heartRateTextView.setText(String.format("%d BPM", heartRate));

                        // Update status and card color
                        updateHeartRateStatus(heartRate);

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
                        heartRateTextView.setText("Invalid format");
                        heartRateStatusTextView.setText("Error");
                        heartRateStatusTextView.setTextColor(Color.RED);
                        heartRateCard.setCardBackgroundColor(Color.parseColor("#FFCCCC"));
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating heart rate for error condition", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateHeartRate", e);
        }
    }

    private void updateHeartRateStatus(int heartRate) {
        // Normal adult resting heart rate: 60-100 BPM
        if (heartRate < 50) {
            // Severe bradycardia
            heartRateStatusTextView.setText("Bradycardia - Very Low");
            heartRateStatusTextView.setTextColor(Color.parseColor("#9C27B0"));  // Purple
            heartRateCard.setCardBackgroundColor(Color.parseColor("#F3E5F5"));  // Light purple
        } else if (heartRate < 60) {
            // Mild bradycardia
            heartRateStatusTextView.setText("Bradycardia");
            heartRateStatusTextView.setTextColor(Color.parseColor("#673AB7"));  // Deep purple
            heartRateCard.setCardBackgroundColor(Color.parseColor("#EDE7F6"));  // Very light purple
        } else if (heartRate <= 100) {
            // Normal
            heartRateStatusTextView.setText("Normal");
            heartRateStatusTextView.setTextColor(Color.parseColor("#4CAF50"));  // Green
            heartRateCard.setCardBackgroundColor(Color.WHITE);
        } else if (heartRate <= 120) {
            // Mild tachycardia
            heartRateStatusTextView.setText("Mild Tachycardia");
            heartRateStatusTextView.setTextColor(Color.parseColor("#FF9800"));  // Orange
            heartRateCard.setCardBackgroundColor(Color.parseColor("#FFF3E0"));  // Very light orange
        } else {
            // Tachycardia
            heartRateStatusTextView.setText("Tachycardia - High");
            heartRateStatusTextView.setTextColor(Color.parseColor("#F44336"));  // Red
            heartRateCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"));  // Very light red
        }
    }

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
                                spo2TextView.setText(String.format("%d%%", spo2));

                                // Save to database
                                saveHealthParameter("spo2", spo2);

                                // Update status and card color
                                updateSpO2Status(spo2);

                            } catch (NumberFormatException nfe) {
                                // If not a number, just display as text
                                spo2TextView.setText(payload);
                                spo2StatusTextView.setText("Unknown");
                                spo2StatusTextView.setTextColor(Color.BLUE);
                                spo2Card.setCardBackgroundColor(Color.WHITE);
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

            // Save to database
            saveHealthParameter("spo2", spo2);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Update SpO2 text
                        spo2TextView.setText(String.format("%d%%", spo2));

                        // Update status and card color
                        updateSpO2Status(spo2);

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
                        spo2TextView.setText("Invalid format");
                        spo2StatusTextView.setText("Error");
                        spo2StatusTextView.setTextColor(Color.RED);
                        spo2Card.setCardBackgroundColor(Color.parseColor("#FFCCCC"));
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating SpO2 for error condition", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateSpO2", e);
        }
    }

    private void updateSpO2Status(int spo2) {
        // Normal SpO2: 95-100%
        if (spo2 < 90) {
            // Hypoxemia (severe)
            spo2StatusTextView.setText("Severe Hypoxemia");
            spo2StatusTextView.setTextColor(Color.parseColor("#D50000"));  // Dark red
            spo2Card.setCardBackgroundColor(Color.parseColor("#FFCDD2"));  // Light red
        } else if (spo2 < 95) {
            // Mild Hypoxemia
            spo2StatusTextView.setText("Mild Hypoxemia");
            spo2StatusTextView.setTextColor(Color.parseColor("#FF9800"));  // Orange
            spo2Card.setCardBackgroundColor(Color.parseColor("#FFF3E0"));  // Very light orange
        } else {
            // Normal
            spo2StatusTextView.setText("Normal");
            spo2StatusTextView.setTextColor(Color.parseColor("#4CAF50"));  // Green
            spo2Card.setCardBackgroundColor(Color.WHITE);
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
                            deviceStatusTextView.setText(payload);
                            deviceStatusTextView.setTextColor(Color.BLUE);
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
                        if (isWorn) {
                            deviceStatusTextView.setText("Device is being worn");
                            deviceStatusTextView.setTextColor(Color.parseColor("#4CAF50"));  // Green
                        } else {
                            deviceStatusTextView.setText("Device is NOT worn");
                            deviceStatusTextView.setTextColor(Color.parseColor("#F44336"));  // Red
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating device status TextView", e);
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing device status data: " + e.getMessage() + ", payload: " + payload, e);

            // Handle error by showing a warning in the UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        deviceStatusTextView.setText("Status: Invalid format");
                        deviceStatusTextView.setTextColor(Color.parseColor("#FF9800"));  // Orange
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating device status for error condition", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateDeviceStatus", e);
        }
    }

    private void updateLocation(String payload) {
        try {
            Log.d(TAG, "updateLocation called with payload: " + payload);

            double latitude;
            double longitude;

            // MQTT is sending comma-separated string values instead of JSON
            if (payload.contains(",")) {
                // Split the comma-separated string
                String[] coordinates = payload.split(",");
                if (coordinates.length >= 2) {
                    try {
                        latitude = Double.parseDouble(coordinates[0].trim());
                        longitude = Double.parseDouble(coordinates[1].trim());
                        Log.d(TAG, "Location parsed from CSV format: " + latitude + ", " + longitude);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing location coordinates from string: " + payload, e);
                        return;
                    }
                } else {
                    Log.e(TAG, "Invalid location format, expected comma-separated values: " + payload);
                    return;
                }
            }
            // If it's JSON, handle it as before
            else if (isValidJson(payload)) {
                JSONObject json = new JSONObject(payload);
                latitude = json.getDouble("lat");
                longitude = json.getDouble("lng");
                Log.d(TAG, "Location parsed from JSON: " + latitude + ", " + longitude);
            }
            // If neither format is detected
            else {
                Log.e(TAG, "Unrecognized location format: " + payload);
                return;
            }

            // Update the current location field
            currentLocation = new LatLng(latitude, longitude);

            // Update location text display on UI thread
            final double finalLatitude = latitude;
            final double finalLongitude = longitude;
            runOnUiThread(() -> {
                locationTextView.setText(String.format("Location: %.6f, %.6f", finalLatitude, finalLongitude));
                locationTextView.setTextColor(Color.BLUE);

                // Update map if it's ready
                if (isMapReady && googleMap != null) {
                    updateMapLocation(finalLatitude, finalLongitude);
                } else {
                    Log.w(TAG, "Map not ready, location update stored for later. isMapReady=" +
                            isMapReady + ", googleMap=" + (googleMap != null ? "not null" : "null"));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateLocation: " + e.getMessage(), e);
        }
    }

    private void updateMapLocation(double latitude, double longitude) {
        // Must run on UI thread, but we're already in runOnUiThread from the calling method
        try {
            Log.d(TAG, "updateMapLocation called with lat=" + latitude + ", lng=" + longitude);

            if (googleMap == null) {
                Log.e(TAG, "googleMap is null, cannot update map");
                return;
            }

            // Create a LatLng object from the coordinates
            LatLng location = new LatLng(latitude, longitude);

            // Clear previous markers
            googleMap.clear();

            // Add a marker for this location
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Patient's Location"));

            // Move the camera to the location with a zoom level
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));

            Log.d(TAG, "Map marker and camera updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error updating map location: " + e.getMessage(), e);
        }
    }

    private void handleMedicationConfirmation(String payload) {
        try {
            Log.d(TAG, "handleMedicationConfirmation called with payload: " + payload);

            // Check if payload is valid JSON
            if (!isValidJson(payload)) {
                Log.w(TAG, "Received non-JSON payload for medication confirmation: " + payload);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Medication update: " + payload, Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }

            // Process as JSON
            JSONObject json = new JSONObject(payload);
            final String medicationName = json.getString("medication");
            final boolean taken = json.getBoolean("taken");

            Log.d(TAG, "Medication confirmation parsed: " + medicationName + " " + (taken ? "taken" : "missed"));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String message = medicationName + (taken ? " taken" : " missed");
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

    private void saveHealthParameter(String type, double value) {
        try {
            // Create a new parameter object
            HealthParameter parameter = new HealthParameter(type, value, System.currentTimeMillis());

            // Insert into database
            database.healthParameterDao().insert(parameter);

            // Keep only the latest 10 entries for this type
            database.healthParameterDao().keepLatest10ByType(type);

            Log.d(TAG, "Saved " + type + " value: " + value + " to database");
        } catch (Exception e) {
            Log.e(TAG, "Error saving health parameter to database", e);
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        Log.d(TAG, "onMapReady called");
        googleMap = map;
        isMapReady = true;

        // Default location - set to a default if no location has been received yet
        LatLng defaultLocation = new LatLng(17.4431, 78.3496);  // Example: Hyderabad

        // Use current location if available, otherwise use default
        LatLng locationToShow = currentLocation != null ? currentLocation : defaultLocation;

        // Add a marker and move the camera
        googleMap.addMarker(new MarkerOptions()
                .position(locationToShow)
                .title("Patient's Location"));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationToShow, 15f));

        Log.d(TAG, "Initial map setup complete");
    }

    @Override
    public void onDeleteClick(Medication medication) {
        // Delete medication from database
        database.medicationDao().delete(medication);

        // Refresh medication list
        loadMedications();

        // Show confirmation
        Toast.makeText(this, "Medication deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttHandler != null) {
            mqttHandler.disconnect();
        }
    }
}