package com.jianxunfuture.juntaida;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.jianxunfuture.juntaidasdk.MqttClient;
import com.jianxunfuture.juntaidasdk.MqttMessageArrivedListener;

public class MqttService extends Service {

    private MqttClient client;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 产品名称，禁止修改
        String productId = "P2XILBH50H";
        // 用户名：产品唯一标识(UUID)
        String username = "862270062139536";
        // 密码：产品Token，从/system/register接口获取得到
        String password = "502E06B1-2AC8-4AF6-8F88-E23CB77E632E";

        client = MqttClient.getInstance();
        client.setBroker("tcp://broker.juntaida.net:1883");
        client.init(this.getApplicationContext(), productId, username, password);
        client.setMessageArrivedListener(new MqttMessageArrivedListener() {
            @Override
            public void messageArrived(String topic, String message) {
                // 接收到来自服务器的数据
                Log.i("test", topic + " " + message);
                // 回复服务器
                client.publishData("From client message! You message:" + message);
            }
        });
        client.connect();

    }
}
