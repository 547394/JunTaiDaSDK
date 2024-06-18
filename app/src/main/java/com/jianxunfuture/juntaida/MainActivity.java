package com.jianxunfuture.juntaida;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jianxunfuture.juntaidasdk.HttpClient;
import com.jianxunfuture.juntaidasdk.HttpResponseCallback;
import com.jianxunfuture.juntaidasdk.OnDownloadListener;
import com.kongzue.baseokhttp.util.JsonList;
import com.kongzue.baseokhttp.util.JsonMap;

import java.io.File;


public class MainActivity extends Activity {

    HttpClient httpClient;

    String Token;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String deviceId   = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        final String deviceName = Build.MODEL;
        final String typeName   = Build.PRODUCT;
        // 启动后台服务
        // startService(new Intent(MainActivity.this, MqttService.class));

        httpClient = new HttpClient(MainActivity.this, "https://can.juntaida.net/api/v1");
        httpClient.init("M48E4K3NYQ69Q0IK", "S7Q6PHUG9AMR2GCT108QBB5WUCEEV6RR");
        // 开启调试模式
        httpClient.setDebug(true);
        // 设置Token, Token 应当由其它永久存储方式获取
        // 伪代码 String Token = SharedPreferences::Get("Token", "")
        // String Token = "58247987-217C-49F6-954E-27F56921B96E"
        httpClient.setToken(Token);
        /*
         * 注册
         */
        Button register = findViewById(R.id.register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                httpClient.register(deviceName, deviceId, typeName, new HttpResponseCallback() {
                    @Override
                    public void onSuccess(JsonMap result) {
                        // 需要永久存储Token以便后续使用
                        // 比如说用 SharedPreferences 存储Token
                        // 伪代码 SharedPreferences::Set("Token", Token)
                        String Token = result.getString("Token");
                        // 更新Token
                        httpClient.setToken(Token);
                    }

                    @Override
                    public void onFail(String error) {
                    }
                });
            }
        });


        /*
         * 注销
         */
        Button unregister = findViewById(R.id.unregister);
        unregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                httpClient.unregister(deviceId, new HttpResponseCallback() {
                    @Override
                    public void onSuccess(JsonMap result) {
                        // 这里可以不用判断result的值，因为该接口总是返回成功
                    }

                    @Override
                    public void onFail(String error) {

                    }
                });
            }
        });

        /*
         * 获取单个文件
         */
        Button getAPKFile = findViewById(R.id.getAPkFile);
        getAPKFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                httpClient.getAPKFile("I2E", 0, new HttpResponseCallback() {
                    @Override
                    public void onSuccess(JsonMap result) {
                        // 文件名
                        String name = result.getString("name");
                        // 文件的sha1值，应当在下载完成后与本地计算的sha1进行比对，如果不一致则重要重新下载
                        String sha1 = result.getString("sha1");
                        // CDN 节点， 如有多个节点，则从第一个开始下载，如果第一个下载失败则下载第二个
                        JsonList cdnNode = result.getList("cdnNode");
                        JsonMap  node    = cdnNode.getJsonMap(0);

                        Log.i("getAPKFile", String.format("fileName:%s sha1:%s, download:%s", name, sha1, node));

                        // TODO:请求读写磁盘权限
                        File file = new File(new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "Download"), name + ".apk");
                        httpClient.download(node.getInt("id"), node.getString("area"), file, new OnDownloadListener() {
                            @Override
                            public void onDownloadSuccess(File file) {
                                Toast.makeText(MainActivity.this, "文件已下载完成：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                // TODO:校验SHA1值
                            }

                            @Override
                            public void onDownloading(int progress) {

                            }

                            @Override
                            public void onDownloadFailed(Exception e) {
                                Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFail(String error) {

                    }
                });
            }
        });

        Button getAPKList = findViewById(R.id.getAPKList);
        getAPKList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int page = 1;
                httpClient.getAPKList("", 0, "", page, new HttpResponseCallback() {
                    @Override
                    public void onSuccess(JsonMap result) {
                        // 文件总数
                        String total = result.getString("total");
                        // 分页大小
                        int size = result.getInt("size");
                        // 是否还有下一页，boolean next = size * page < total
                        // 请求下一页 page = page+1 再次调用该方法
                        JsonList rows = result.getList("rows");
                        for (int i = 0; i < rows.size(); i++) {
                            // 请参见 108 行的代码结构
                            Log.i("getAPKList", rows.getJsonMap(i).getString("name"));
                        }
                    }

                    @Override
                    public void onFail(String error) {

                    }
                });
            }
        });
        /*
         * 反馈
         */
        Button report = findViewById(R.id.report);
        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                httpClient.report("test data", new HttpResponseCallback() {
                    @Override
                    public void onSuccess(JsonMap result) {

                    }

                    @Override
                    public void onFail(String error) {

                    }
                });
            }
        });

    }
}