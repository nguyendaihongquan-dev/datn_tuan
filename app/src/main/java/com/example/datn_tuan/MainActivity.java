package com.example.datn_tuan;

import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
//
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MQTT_CLIENT";
    private static final String HOST = "50a989b3f1d24fbfa84a1e80f65a0e0a.s1.eu.hivemq.cloud";
    private static final String USERNAME = "tuan.pa203636";
    private static final String PASSWORD = "Matkhau123";
    private static String tempLiving = "0";
    private static  String humiLiving = "0";
    private static  String tempBedRoom = "0";
    private static  String humiBedRoom = "0";
    private static  List<String> listTopic = Arrays.asList("living/humidity","living/temperature","living/light","living/stair", "door/control", "BaoChay","bedroom/temperature","bedroom/humidity","bedroom/curtain","bedroom/light","bedroom/fan","kitchen/gas/gasThreshold","kitchen/gas/warning");
    private Mqtt5BlockingClient client;
    private StorageReference mStorageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textTempLiving = findViewById(R.id.tempLiving);
        textTempLiving.setText(tempLiving);
        TextView textHumiLiving = findViewById(R.id.humiLiving);
        textHumiLiving.setText(humiLiving);
        TextView textTempBedRoom = findViewById(R.id.tempBedRoom);
        textTempBedRoom.setText(tempBedRoom);
        TextView textHumiBedRoom = findViewById(R.id.humiBedRoom);
        textHumiBedRoom.setText(humiBedRoom);

        // mqtt
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
        SwitchCompat buttonStairBedLiving= findViewById(R.id.stairLiving);
        buttonStairBedLiving.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "true" : "false";;
            publishMessage(message, "living/stair");
        });
        SwitchCompat buttonFire= findViewById(R.id.buttonFire);
        buttonFire.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "true" : "false";;
            publishMessage(message, "BaoChay");
        });
        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        Button button3 = findViewById(R.id.button3);
        button1.setOnClickListener(v ->
                publishMessage("1", "bedroom/curtain")
        );

        button2.setOnClickListener(v ->
                publishMessage("2", "bedroom/curtain")

        );

        button3.setOnClickListener(v ->
                publishMessage("3", "bedroom/curtain")
        );
        Button gas1 = findViewById(R.id.gas1);
        Button gas2 = findViewById(R.id.gas2);
        Button gas3 = findViewById(R.id.gas3);
        button1.setOnClickListener(v ->
                publishMessage("1500", "kitchen/gas/gasThreshold")
        );

        button2.setOnClickListener(v ->
                publishMessage("3000", "kitchen/gas/gasThreshold")
        );

        button3.setOnClickListener(v ->
                publishMessage("4095", "kitchen/gas/gasThreshold")
        );
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
                            }else if(message.contains("{\"fire_detected\":true}")){
                                runOnUiThread(() -> {
                                    // Tạo AlertDialog Builder
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("Cảnh báo hỏa hoạn")
                                            .setMessage("Phát hiện cháy! Vui lòng kiểm tra ngay.")
                                            .setIcon(android.R.drawable.ic_dialog_alert) // Icon mặc định
                                            .setPositiveButton("OK", (dialog, which) -> {
                                                // Hành động khi bấm OK, nếu cần
                                                dialog.dismiss();
                                            })
                                            .setCancelable(false) // Không cho phép tắt bằng cách bấm ra ngoài
                                            .show();
                                });
                            }else if(message.contains("{\"smoke_detected\":true}")){
                                runOnUiThread(() -> {
                                    showImageDialog();
                                    // Tạo AlertDialog Builder
//                                    new AlertDialog.Builder(MainActivity.this)
//                                            .setTitle("Cảnh báo khói")
//                                            .setMessage("Phát hiện khói! Vui lòng kiểm tra ngay.")
//                                            .setIcon(android.R.drawable.ic_dialog_alert) // Icon mặc định
//                                            .setPositiveButton("OK", (dialog, which) -> {
//                                                // Hành động khi bấm OK, nếu cần
//                                                dialog.dismiss();
//                                            })
//                                            .setCancelable(false) // Không cho phép tắt bằng cách bấm ra ngoài
//                                            .show();
                                });
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
                        case "living/stair":
                            if (message.contains("false")) {
                                SwitchCompat switchCompat = findViewById(R.id.stairLiving);
                                switchCompat.setChecked(false); // Gạt lại SwitchCompat về trạng thái OFF
                            } else if(message.contains("true")){
                                SwitchCompat switchCompat = findViewById(R.id.stairLiving);
                                switchCompat.setChecked(true);
                            }
                            break;
                        case "living/temperature":
                            TextView textTempLiving = findViewById(R.id.tempLiving);
                            textTempLiving.setText(message);
                            break;
                        case "living/humidity":
                            TextView textHumiLiving = findViewById(R.id.humiLiving);
                            textHumiLiving.setText(message);
                            break;
                        case "bedroom/temperature":
                            TextView textTempBedRoom = findViewById(R.id.tempBedRoom);
                            textTempBedRoom.setText(message);
                            break;
                        case "bedroom/humidity":
                            TextView textHumiBedRoom = findViewById(R.id.humiBedRoom);
                            textHumiBedRoom.setText(message);
                            break;

                        case "kitchen/gas/warning":
                            if(message.contains("true")){
                                runOnUiThread(() -> {
                                    // Tạo AlertDialog Builder
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle("Cảnh báo khí gas vượt ngưỡng")
                                            .setMessage("Phát hiện khí gas! Vui lòng kiểm tra ngay.")
                                            .setIcon(android.R.drawable.ic_dialog_alert) // Icon mặc định
                                            .setPositiveButton("OK", (dialog, which) -> {
                                                // Hành động khi bấm OK, nếu cần
                                                dialog.dismiss();
                                            })
                                            .setCancelable(false) // Không cho phép tắt bằng cách bấm ra ngoài
                                            .show();
                                });
                            }
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
    private void showImageDialog() {
        // Tạo một Dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image);
        dialog.setCancelable(true);

        // Tạo một ProgressBar để hiển thị trong lúc tải ảnh
        final ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
        final ImageView imageView = dialog.findViewById(R.id.imageView);

        // Lấy ảnh từ Firebase Storage
        StorageReference imageRef = mStorageRef.child("images/detection_latest.jpg");

        // Sử dụng Picasso để tải ảnh và hiển thị
        Picasso.get()
                .load(String.valueOf(imageRef))
                .into(imageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
//                        imageView.setImageResource(R.drawable.error_image); // Nếu lỗi
                    }
                });

        // Hiển thị Dialog
        dialog.show();
    }
}