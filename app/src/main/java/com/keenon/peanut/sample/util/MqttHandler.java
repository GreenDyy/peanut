package com.keenon.peanut.sample.util;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class MqttHandler {
    private static final String TAG = "MqttHandler";

    private MqttAsyncClient client;

    public void connect(String brokerUrl, String clientId, String username, String password, IMqttActionListener listener, MqttCallback callback) {
        try {
            client = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());

            // options
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setUserName(username);
            connectOptions.setPassword(password.toCharArray());
            connectOptions.setCleanSession(true);
            connectOptions.setAutomaticReconnect(true);
            connectOptions.setKeepAliveInterval(60);
            connectOptions.setConnectionTimeout(30);

            // SSL nếu port 8883
            if (brokerUrl.startsWith("ssl://") || brokerUrl.contains(":8883")) {
                try {
                    TrustManager[] trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                            }
                    };

                    SSLContext sc = SSLContext.getInstance("SSL");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
                    connectOptions.setSocketFactory(sslSocketFactory);
                    Log.d(TAG, "🔒 SSL/TLS configured");
                } catch (Exception sslEx) {
                    Log.e(TAG, "❌ SSL config error", sslEx);
                }
            }

            // set callback từ Activity
            if (callback != null) {
                client.setCallback(callback);
            }

            // connect async
            client.connect(connectOptions, null, listener);

        } catch (MqttException e) {
            Log.e(TAG, "❌ MQTT connect error", e);
            if (listener != null) {
                listener.onFailure(null, e);
            }
        }
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                Log.d(TAG, "🔌 Disconnected");
            }
        } catch (MqttException e) {
            Log.e(TAG, "❌ Disconnect error", e);
        }
    }

    public void publish(String topic, String message) {
        try {
            if (client != null && client.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttMessage.setQos(1);
                mqttMessage.setRetained(false);
                client.publish(topic, mqttMessage);
                Log.d(TAG, "📤 Published [" + topic + "]: " + message);
            } else {
                Log.w(TAG, "⚠️ Not connected, cannot publish");
            }
        } catch (MqttException e) {
            Log.e(TAG, "❌ Publish error", e);
        }
    }

    public void subscribe(String topic) {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(topic, 1);
                Log.d(TAG, "📡 Subscribed: " + topic);
            } else {
                Log.w(TAG, "⚠️ Not connected, cannot subscribe");
            }
        } catch (MqttException e) {
            Log.e(TAG, "❌ Subscribe error", e);
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
