package com.jianxunfuture.juntaidasdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttClient {

    public static final String BROADCAST_MQTT_CONNECTING      = "com.jianxunfuture.mqtt.connecting";
    public static final String BROADCAST_MQTT_CONNECTED       = "com.jianxunfuture.mqtt.connected";
    public static final String BROADCAST_MQTT_CONNECTION_LOST = "com.jianxunfuture.mqtt.connectionLost";


    private       MqttAndroidClient  mqttClient;
    private       MqttConnectOptions mqttOptions;
    private       Context            context;
    private final String             broker = "tcp://broker.juntaida.net:1883";
    private       String             publishTopicControl;
    private       String             publishTopicData;

    private boolean isManualDisConnected;


    private MqttMessageArrivedListener messageArrivedListener;

    private final String TAG = this.getClass().getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private static MqttClient instance;

    private MqttClient() {
    }

    public static MqttClient getInstance() {
        if (null == instance) {
            synchronized (MqttClient.class) {
                if (null == instance) {
                    instance = new MqttClient();
                }
            }
        }
        return instance;
    }

    public void init(Context context, String productId, String username, String password) {
        init(context, productId, username, password, 8, 30, true);
    }

    public void init(final Context context, String productId, String username, String password, int connectionTimeout, int keepAliveInterval, boolean cleanSession) {
        // 配置连接
        mqttOptions = new MqttConnectOptions();
        mqttOptions.setConnectionTimeout(connectionTimeout);
        mqttOptions.setKeepAliveInterval(keepAliveInterval);
        mqttOptions.setCleanSession(cleanSession);
        mqttOptions.setUserName(username);
        mqttOptions.setPassword(password.toCharArray());
        // 这个重连机制不科学，重写重连机制
        mqttOptions.setAutomaticReconnect(false);
        // 设备连接参数
        mqttClient = new MqttAndroidClient(context, broker, username, new MemoryPersistence());
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                sendBroadcast(BROADCAST_MQTT_CONNECTION_LOST, "connect_lost");
                if (!isManualDisConnected) {
                    connect();
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Log.i("messageArrived", "topic:" + topic + ", message:" + message.toString());
                if (messageArrivedListener != null) {
                    try {
                        messageArrivedListener.messageArrived(topic, message.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.i("messageArrived", "messageArrivedListener is null");
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
        publishTopicData    = String.format("%s/%s/data", productId, username);
        publishTopicControl = String.format("%s/%s/control", productId, username);
        this.context        = context;
        Log.i(TAG, String.format("broker:%s, productId:%s, deviceSN:%s", broker, productId, username));
    }

    public void connect() {
        isManualDisConnected = false;
        try {
            sendBroadcast(BROADCAST_MQTT_CONNECTING, "正在连接服务器...");
            mqttClient.connect(mqttOptions, context, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    subscribeTopic();
                    sendBroadcast(BROADCAST_MQTT_CONNECTED, "服务器已连接");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (exception.toString().contains("已在进行连接")) {
                        sendBroadcast(BROADCAST_MQTT_CONNECTING, "正在连接服务器...");
                        return;
                    } else if (exception.toString().contains("Host")) {
                        sendBroadcast(BROADCAST_MQTT_CONNECTION_LOST, "没有网络, 设备当前离线");
                    } else {
                        sendBroadcast(BROADCAST_MQTT_CONNECTION_LOST, exception.toString());
                    }
                    Log.e(TAG, exception.toString());
                    if (!mqttClient.isConnected()) {
                        new Handler().postDelayed(() -> connect(), 10 * 1000);
                    }
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 订阅主题
    private void subscribeTopic() {
        // 设置断连状态 buffer 缓冲区
        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(1024);
        bufferOptions.setDeleteOldestMessages(true);
        mqttClient.setBufferOpts(bufferOptions);
        try {
            mqttClient.subscribe(publishTopicControl, 0);
        } catch (Exception e) {
            Log.e(TAG, "订阅主题失败");
        }
    }

    public void setMessageArrivedListener(MqttMessageArrivedListener messageArrivedListener) {
        this.messageArrivedListener = messageArrivedListener;
    }

    private void sendBroadcast(String broadcast, String message) {
        Intent intent = new Intent(broadcast);
        intent.putExtra("message", message);
        context.sendBroadcast(intent);
        Log.i(TAG, message);
    }

    public void publishData(String data) {
        publish(publishTopicData, data, 0);
    }

    private void publish(String topic, String msg, int qos) {
        if (isConnected()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(msg.getBytes());
            mqttMessage.setRetained(false);
            mqttMessage.setQos(qos);
            try {
                mqttClient.publish(topic, mqttMessage);
                Log.i(TAG, "publish:" + topic + " => " + msg);
            } catch (MqttException e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            Log.e(TAG, "publish failed:" + msg);
        }
    }

    public void disConnect() {
        if (null == mqttClient) {
            return;
        }
        try {
            mqttClient.disconnect();
            context.sendBroadcast(new Intent(BROADCAST_MQTT_CONNECTION_LOST));
        } catch (MqttException e) {
            e.printStackTrace();
        }
        isManualDisConnected = true;
    }

    public boolean isConnected() {
        if (null == mqttClient) {
            return false;
        }
        return mqttClient.isConnected();
    }
}
