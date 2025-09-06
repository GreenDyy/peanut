package com.keenon.peanut.sample.test;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.keenon.peanut.sample.R;
import com.keenon.peanut.sample.util.BaseActivity;
import com.keenon.peanut.sample.util.MqttHandler;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MqttDemoActivity extends BaseActivity {

    private static final String TAG = "MqttDemoActivity";

    // MQTT connection settings
    private static final String BROKER_URL = "ssl://emqx.naiscorp.com:8883";
    private static final String CLIENT_ID = "a8faad3e-44fe-41d6-8084-d0ad7ce6dcd9";
    private static final String USER_NAME = "robot01";
    private static final String PASSWORD = "E9SvBhWXK6ZL0z89";
    private static final String DEFAULT_TOPIC = "robot/signal/a8faad3e-44fe-41d6-8084-d0ad7ce6dcd9";

    @BindView(R.id.tv_status) TextView tvStatus;
    @BindView(R.id.tv_messages) TextView tvMessages;
    @BindView(R.id.et_topic) EditText etTopic;
    @BindView(R.id.et_message) EditText etMessage;
    @BindView(R.id.btn_connect) Button btnConnect;
    @BindView(R.id.btn_disconnect) Button btnDisconnect;
    @BindView(R.id.btn_subscribe) Button btnSubscribe;
    @BindView(R.id.btn_publish) Button btnPublish;
    @BindView(R.id.btn_clear) Button btnClear;

    private MqttHandler mqttHandler;
    private StringBuilder messageLog = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt_demo);
        ButterKnife.bind(this);

        initMqtt();
        initView();
    }

    private void initView() {
        setTitle("MQTT Demo");

        etTopic.setText(DEFAULT_TOPIC);
        etMessage.setText("Hello MQTT from Android!");

        tvMessages.setMovementMethod(new ScrollingMovementMethod());

        updateUIState();
        addLog("🚀 MQTT Demo khởi động");
    }

    private void initMqtt() {
        mqttHandler = new MqttHandler();
    }

    private IMqttActionListener mqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            addLog("✅ MQTT connect success");
            runOnUiThread(() -> updateUIState());
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            addLog("❌ MQTT connect failed: " + (exception != null ? exception.getMessage() : "Unknown"));
            runOnUiThread(() -> updateUIState());
        }
    };

    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            addLog("🚨 MQTT connection lost: " + (cause != null ? cause.getMessage() : "Unknown"));
            runOnUiThread(() -> updateUIState());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            String payload = new String(message.getPayload());
            addLog("📩 Message arrived [" + topic + "]: " + payload);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            addLog("✅ Message delivered");
        }
    };

    @OnClick(R.id.btn_connect)
    public void onConnectClick() {
        if (mqttHandler == null) return;

        if (!mqttHandler.isConnected()) {
            addLog("🔌 Đang kết nối đến MQTT broker...");
            mqttHandler.connect(BROKER_URL, CLIENT_ID, USER_NAME, PASSWORD, mqttActionListener, mqttCallback);
        } else {
            Toast.makeText(this, "Đã kết nối rồi!", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btn_disconnect)
    public void onDisconnectClick() {
        if (mqttHandler != null && mqttHandler.isConnected()) {
            mqttHandler.disconnect();
            updateUIState();
            addLog("🔌 Đã ngắt kết nối MQTT");
        } else {
            Toast.makeText(this, "Chưa kết nối!", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btn_subscribe)
    public void onSubscribeClick() {
        if (mqttHandler == null || !mqttHandler.isConnected()) {
            Toast.makeText(this, "Vui lòng kết nối trước!", Toast.LENGTH_SHORT).show();
            addLog("⚠️ Chưa kết nối MQTT, không thể subscribe");
            return;
        }

        String topic = etTopic.getText().toString().trim();
        if (topic.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập topic!", Toast.LENGTH_SHORT).show();
            return;
        }

        mqttHandler.subscribe(topic);
        addLog("📡 Đã subscribe topic: " + topic);
    }

    @OnClick(R.id.btn_publish)
    public void onPublishClick() {
        if (mqttHandler == null || !mqttHandler.isConnected()) {
            Toast.makeText(this, "Vui lòng kết nối trước!", Toast.LENGTH_SHORT).show();
            addLog("⚠️ Chưa kết nối MQTT, không thể publish");
            return;
        }

        String topic = etTopic.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (topic.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập topic và message!", Toast.LENGTH_SHORT).show();
            return;
        }

        mqttHandler.publish(topic, message);
        addLog("📤 Published [" + topic + "]: " + message);
    }

    @OnClick(R.id.btn_clear)
    public void onClearClick() {
        messageLog.setLength(0);
        tvMessages.setText("");
        addLog("🧹 Đã xóa log");
    }

    private void updateUIState() {
        boolean connected = mqttHandler != null && mqttHandler.isConnected();

        btnConnect.setEnabled(!connected);
        btnDisconnect.setEnabled(connected);
        btnSubscribe.setEnabled(connected);
        btnPublish.setEnabled(connected);

        tvStatus.setText(connected ? "🟢 Đã kết nối" : "🔴 Chưa kết nối");
        tvStatus.setTextColor(connected ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_red_dark));
    }

    private void addLog(String message) {
        String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";
        messageLog.append(logEntry);

        runOnUiThread(() -> {
            tvMessages.setText(messageLog.toString());
            tvMessages.post(() -> {
                int scrollAmount = tvMessages.getLayout().getLineTop(tvMessages.getLineCount()) - tvMessages.getHeight();
                if (scrollAmount > 0) {
                    tvMessages.scrollTo(0, scrollAmount);
                } else {
                    tvMessages.scrollTo(0, 0);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttHandler != null && mqttHandler.isConnected()) {
            mqttHandler.disconnect();
        }
    }
}
