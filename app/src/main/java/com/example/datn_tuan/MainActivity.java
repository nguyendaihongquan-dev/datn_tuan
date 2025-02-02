package com.example.datn_tuan;

import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
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

import de.hdodenhof.circleimageview.CircleImageView;

//
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MQTT_CLIENT";
    private static final String HOST = "50a989b3f1d24fbfa84a1e80f65a0e0a.s1.eu.hivemq.cloud";
    private static final String USERNAME = "tuan.pa203636";
    private static final String PASSWORD = "Matkhau123";
    private static String tempLiving = "0";
    private static String humiLiving = "0";
    private static String tempBedRoom = "0";
    private static String humiBedRoom = "0";
    private static String gasValue = "0";
    private static List<String> listTopic = Arrays.asList("living/humidity", "living/temperature", "living/light", "living/stair", "door/control", "BaoChay", "bedroom/temperature", "bedroom/humidity", "bedroom/curtain", "bedroom/light", "bedroom/fan", "kitchen/gas/gasThreshold", "kitchen/gas/warning","kitchen/gas/value");
    private Mqtt5BlockingClient client;
    private StorageReference mStorageRef;
//    {"smoke_detected":true} bản tin canhr báo
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        mStorageRef = FirebaseStorage.getInstance().getReference();
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
        SwitchCompat buttonLedBedRoom = findViewById(R.id.bedroomLight);
        buttonLedBedRoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "true" : "false";
            publishMessage(message, "bedroom/light");
        });
        SwitchCompat buttonFanBedRoom = findViewById(R.id.bedroomFan);
        buttonFanBedRoom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "true" : "false";
            ;
            publishMessage(message, "bedroom/fan");
        });
        SwitchCompat buttonStairBedLiving = findViewById(R.id.stairLiving);
        buttonStairBedLiving.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "true" : "false";
            ;
            publishMessage(message, "living/stair");
        });
        SwitchCompat buttonFire = findViewById(R.id.buttonFire);
        buttonFire.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "true" : "false";
            ;
            publishMessage(message, "BaoChay");
        });
        Button button1 = findViewById(R.id.sendValue);
        EditText gasValue = findViewById(R.id.valueGas);

        button1.setOnClickListener(v -> {
                    String gas = gasValue.getText().toString().trim();
                    publishMessage(gas, "kitchen/gas/gasThreshold");
                }
        );
        Button btnMuc0 = findViewById(R.id.btn_muc_0);
        Button btnMuc1 = findViewById(R.id.btn_muc_1);
        Button btnMuc2 = findViewById(R.id.btn_muc_2);
        Button btnMuc3 = findViewById(R.id.btn_muc_3);

        btnMuc0.setOnClickListener(v -> {
                    publishMessage("0", "bedroom/curtain");
                }
        );
        btnMuc1.setOnClickListener(v -> {
                    publishMessage("1", "bedroom/curtain");
                }
        );
        btnMuc2.setOnClickListener(v -> {
                    publishMessage("2", "bedroom/curtain");
                }
        );
        btnMuc3.setOnClickListener(v -> {
                    publishMessage("3", "bedroom/curtain");
                }
        );
        CircleImageView avatarImageView = findViewById(R.id.avatarImageView);

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        Button buttonTest = findViewById(R.id.test_log);


        buttonTest.setOnClickListener(v -> {
            showImageDialog();
                }
        );

    }
    private void logout() {
        // Xóa dữ liệu đăng nhập (ví dụ: SharedPreferences)
        getSharedPreferences("user_prefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Hiển thị thông báo
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

        // Chuyển về màn hình đăng nhập
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
            for (int i = 0; i < listTopic.size(); i++) {
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
                            } else if (message.contains("true")) {
                                SwitchCompat switchCompat = findViewById(R.id.livingLight);
                                switchCompat.setChecked(true);
                            }
                            break;
                        case "door/control":
                            if (message.contains("{\"door_status\":false}")) {
                                SwitchCompat switchCompat = findViewById(R.id.livingDoor);
                                switchCompat.setChecked(false); // Gạt lại SwitchCompat về trạng thái OFF
                            } else if (message.contains("{\"door_status\":true}")) {
                                SwitchCompat switchCompat = findViewById(R.id.livingDoor);
                                switchCompat.setChecked(true);
                            } else if (message.contains("{\"fire_detected\":true}")) {
//                                runOnUiThread(() -> {
//                                    // Tạo AlertDialog Builder
//                                    new AlertDialog.Builder(MainActivity.this)
//                                            .setTitle("Cảnh báo hỏa hoạn")
//                                            .setMessage("Phát hiện cháy! Vui lòng kiểm tra ngay.")
//                                            .setIcon(android.R.drawable.ic_dialog_alert) // Icon mặc định
//                                            .setPositiveButton("OK", (dialog, which) -> {
//                                                // Hành động khi bấm OK, nếu cần
//                                                dialog.dismiss();
//                                            })
//                                            .setCancelable(false) // Không cho phép tắt bằng cách bấm ra ngoài
//                                            .show();
//                                });
                                runOnUiThread(this::showImageDialog);
                            } else if (message.contains("{\"smoke_detected\":true}")) {
                                runOnUiThread(this::showImageDialog);
                            }
                            break;
                        case "bedroom/light":
                            if (message.contains("false")) {
                                SwitchCompat switchCompat = findViewById(R.id.bedroomLight);
                                switchCompat.setChecked(false); // Gạt lại SwitchCompat về trạng thái OFF
                            } else if (message.contains("true")) {
                                SwitchCompat switchCompat = findViewById(R.id.bedroomLight);
                                switchCompat.setChecked(true);
                            }
                            break;
                        case "bedroom/fan":
                            if (message.contains("false")) {
                                SwitchCompat switchCompat = findViewById(R.id.bedroomFan);
                                switchCompat.setChecked(false); // Gạt lại SwitchCompat về trạng thái OFF
                            } else if (message.contains("true")) {
                                SwitchCompat switchCompat = findViewById(R.id.bedroomFan);
                                switchCompat.setChecked(true);
                            }
                            break;
                        case "living/stair":
                            if (message.contains("false")) {
                                SwitchCompat switchCompat = findViewById(R.id.stairLiving);
                                switchCompat.setChecked(false); // Gạt lại SwitchCompat về trạng thái OFF
                            } else if (message.contains("true")) {
                                SwitchCompat switchCompat = findViewById(R.id.stairLiving);
                                switchCompat.setChecked(true);
                            }
                            break;
                        case "living/temperature":
                            TextView textTempLiving = findViewById(R.id.tempLiving);
                            textTempLiving.setText(message+"°C");
                            break;
                        case "living/humidity":
                            TextView textHumiLiving = findViewById(R.id.humiLiving);
                            textHumiLiving.setText(message+"%");
                            break;
                        case "bedroom/temperature":
                            TextView textTempBedRoom = findViewById(R.id.tempBedRoom);
                            textTempBedRoom.setText(message+"°C");
                            break;
                        case "bedroom/humidity":
                            TextView textHumiBedRoom = findViewById(R.id.humiBedRoom);
                            textHumiBedRoom.setText(message+"%");
                            break;
                        case "kitchen/gas/value" :
                            TextView textGasValue = findViewById(R.id.valueGasSensor);
                            textGasValue.setText(message);
                            break;
                        case "kitchen/gas/warning":
                            if (message.contains("true")) {
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
        try {
            Log.d("ImageDialog", "Bắt đầu showImageDialog");

            // Tạo một Dialog
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_image);
            dialog.setCancelable(true);
            Log.d("ImageDialog", "Dialog được khởi tạo thành công");

            // Tạo một ProgressBar để hiển thị trong lúc tải ảnh
            final ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
            final ImageView imageView = dialog.findViewById(R.id.imageView);
            Log.d("ImageDialog", "ProgressBar và ImageView được tìm thấy trong layout");

            // Lấy ảnh từ Firebase Storage
            StorageReference imageRef = mStorageRef.child("images/detection_latest.jpg");
            Log.d("ImageDialog", "StorageReference được khởi tạo: " + imageRef.getPath());

            // Sử dụng Picasso để tải ảnh và hiển thị
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d("ImageDialog", "Download URL: " + downloadUrl);

                // Tải ảnh bằng Picasso
                Picasso.get()
                        .load(downloadUrl)
                        .into(imageView, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d("ImageDialog", "Ảnh được tải thành công");
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Exception e) {
                                progressBar.setVisibility(View.GONE);
                                Log.e("ImageDialog", "Lỗi khi tải ảnh bằng Picasso", e);
//                                imageView.setImageResource(R.drawable.error_image); // Nếu lỗi
                            }
                        });
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Log.e("ImageDialog", "Lỗi khi lấy download URL", e);
                Toast.makeText(this, "Không thể tải ảnh từ Firebase Storage.", Toast.LENGTH_SHORT).show();
            });


            // Hiển thị Dialog
            dialog.show();
            Log.d("ImageDialog", "Dialog được hiển thị");
        } catch (Exception e) {
            // Log lỗi và thông báo
            Log.e("ImageDialog", "Error showing image dialog: ", e);
            Toast.makeText(this, "Đã xảy ra lỗi khi hiển thị ảnh.", Toast.LENGTH_SHORT).show();
        }
    }
}