package com.jianxunfuture.juntaidasdk;

public interface MqttMessageArrivedListener {
    void messageArrived(String topic, String message);
}
