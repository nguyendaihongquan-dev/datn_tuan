package com.example.datn_tuan;

import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MQTT_CLIENT";
    private static final String HOST = "91f23c134c8849b5939188b245411169.s1.eu.hivemq.cloud";
    private static final String USERNAME = "nguyenquan";
    private static final String PASSWORD = "!@#QWEasdzxc123";
    private static final String tempLiving = "0";
    private static final String humiLiving = "0";
    private static final String tempBedRoom = "0";
    private static final String humiBedRoom = "0";
    private static final List<String> listTopic = Arrays.asList("living/light","living/stair", "door/control", "BaoChay","bedroom/temperature","bedroom/humidity","bedroom/curtain","bedroom/light","bedroom/fan","kitchen/gas/gasThreshold","kitchen/gas/warning");
    private Mqtt5BlockingClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(this::connectMqtt).start();
        SwitchCompat buttonLedLiving = findViewById(R.id.livingLight);
        buttonLedLiving.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "true" : "false";
            publishMessage(message, "living/light");
        });
        SwitchCompat buttonDoorLiving = findViewById(R.id.livingDoor);
        buttonDoorLiving.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "{\"open_door\":true}" : "{\"close_door\":true}";
            publishMessage(message, "door/control");
        });
        SwitchCompat buttonLedBedRoom= findViewById(R.id.bedroomLight);
        buttonLedBedRoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "true" : "false";
            publishMessage(message, "bedroom/light");
        });
        SwitchCompat buttonFanBedRoom= findViewById(R.id.bedroomFan);
        buttonFanBedRoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "true" : "false";;
            publishMessage(message, "bedroom/fan");
        });
//        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
//        bottomNav.setOnItemSelectedListener(item -> {
//            int itemId = item.getItemId();
//            if (itemId == R.id.nav_home) {
//                // Handle home click
//                return true;
//            } else if (itemId == R.id.nav_device) {
//                // Handle device click
//                return true;
//            } else if (itemId == R.id.nav_voice) {
//                return true;
//            } else if (itemId == R.id.nav_routine) {
//                return true;
//            } else if (itemId == R.id.nav_stats) {
//                return true;
//            }
//            return false;
//        });

//        Button btnPublish = findViewById(R.id.btn_publish);
//        btnPublish.setOnClickListener(v -> {
//            // Gửi tin nhắn lên broker
//            publishMessage("Hello from Android!");
//        });
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
           for(int i =0;i<listTopic.size();i++){
               client.subscribeWith()
                       .topicFilter(listTopic.get(i))
                       .send();
           }
//            client.subscribeWith()
//                    .topicFilter(TOPIC)
//                    .send();

            // Set callback for received messages
            client.toAsync().publishes(ALL, publish -> {
                String topic = publish.getTopic().toString();
                String message = StandardCharsets.UTF_8.decode(publish.getPayload().get()).toString();
                Log.d(TAG, "Received: " + publish.getTopic() + " -> " + message);

                runOnUiThread(() -> {
                    switch (topic) {
                        case "living/light":
                            if (message.contains("false")) {
                                SwitchCompat switchCompat = findViewById(R.id.livingLight);
                                switchCompat.setChecked(false); // Gạt lại SwitchCompat về trạng thái OFF
                            } else if(message.contains("true")){
                                SwitchCompat switchCompat = findViewById(R.id.livingLight);
                                switchCompat.setChecked(true);
                            }
                                break;
                        case "door/control":
                            if (message.contains("{\"door_status\":false}")) {
                                SwitchCompat switchCompat = findViewById(R.id.livingDoor);
                                switchCompat.setChecked(false); // Gạt lại SwitchCompat về trạng thái OFF
                            } else if(message.contains("{\"door_status\":true}")){
                                SwitchCompat switchCompat = findViewById(R.id.livingDoor);
                                switchCompat.setChecked(true);
                            }
                            break;
                        case "bedroom/light":
                            if (message.contains("false")) {
                                SwitchCompat switchCompat = findViewById(R.id.bedroomLight);
                                switchCompat.setChecked(false); // Gạt lại SwitchCompat về trạng thái OFF
                            } else if(message.contains("true")){
                                SwitchCompat switchCompat = findViewById(R.id.bedroomLight);
                                switchCompat.setChecked(true);
                            }
                            break;
                        case "bedroom/fan":
                            if (message.contains("false")) {
                                SwitchCompat switchCompat = findViewById(R.id.bedroomFan);
                                switchCompat.setChecked(false); // Gạt lại SwitchCompat về trạng thái OFF
                            } else if(message.contains("true")){
                                SwitchCompat switchCompat = findViewById(R.id.bedroomFan);
                                switchCompat.setChecked(true);
                            }
                            break;
                        case "bedroom/fan":

                    }
                });
            });

        } catch (Exception e) {
            Log.e(TAG, "MQTT Error: ", e);
        }
    }

    public void publishMessage(String message, String topic) {
        if (client != null && client.getState().isConnected()) {
            try {
                client.publishWith()
                        .topic(topic)
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