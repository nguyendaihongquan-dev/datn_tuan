package com.example.datn_tuan;

import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MQTT_CLIENT";
    private static final String HOST = "91f23c134c8849b5939188b245411169.s1.eu.hivemq.cloud";
    private static final String USERNAME = "nguyenquan";
    private static final String PASSWORD = "!@#QWEasdzxc123";
    private static final String TOPIC = "esp32/humi";

    private Mqtt5BlockingClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(this::connectMqtt).start();
        Button btnPublish = findViewById(R.id.btn_publish);
        btnPublish.setOnClickListener(v -> {
            // Gửi tin nhắn lên broker
            publishMessage("Hello from Android!");
        });
    }

    private void connectMqtt() {
        try {
            client = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost(HOST)
                    .serverPort(8883)
                    .sslWithDefaultConfig()
                    .buildBlocking();

            client.connectWith()
                    .simpleAuth()
                    .username(USERNAME)
                    .password(StandardCharsets.UTF_8.encode(PASSWORD))
                    .applySimpleAuth()
                    .send();

            Log.d(TAG, "Connected successfully");

            // Subscribe to topic
            client.subscribeWith()
                    .topicFilter(TOPIC)
                    .send();

            // Set callback for received messages
            client.toAsync().publishes(ALL, publish -> {
                String message = StandardCharsets.UTF_8.decode(publish.getPayload().get()).toString();
                Log.d(TAG, "Received: " + publish.getTopic() + " -> " + message);

                runOnUiThread(() -> {
                    // Update UI here if needed
                });
            });

        } catch (Exception e) {
            Log.e(TAG, "MQTT Error: ", e);
        }
    }

    public void publishMessage(String message) {
        if (client != null && client.getState().isConnected()) {
            try {
                client.publishWith()
                        .topic(TOPIC)
                        .payload(StandardCharsets.UTF_8.encode(message))
                        .send();
                Log.d(TAG, "Published: " + message);
            } catch (Exception e) {
                Log.e(TAG, "Publish Error: ", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client != null) {
            client.disconnect();
        }
    }
}