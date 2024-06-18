package com.jianxunfuture.juntaidasdk;

import android.content.Context;

import com.kongzue.baseokhttp.HttpRequest;
import com.kongzue.baseokhttp.listener.JsonResponseListener;
import com.kongzue.baseokhttp.util.BaseOkHttp;
import com.kongzue.baseokhttp.util.JsonMap;
import com.kongzue.baseokhttp.util.Parameter;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class HttpClient {

    private final Context context;
    private       String  secretId;
    private       String  secretKey;
    private       String  Token;

    /**
     * 构造函数
     *
     * @param context 建议在传入 context 时，使用 context.getApplicationContext() 方法来强制异步线程返回，待数据得到妥善处理后，
     *                需要返回 UI 线程刷新界面显示时，使用 activity.runOnUiThread(...)切换到主线程进行刷新。
     * @param servlet 设置全局请求地址，所有后续接口都使用相对地址
     */
    public HttpClient(Context context, String servlet) {
        this.context          = context;
        BaseOkHttp.serviceUrl = servlet;
    }

    public void setDebug(Boolean debug) {
        BaseOkHttp.DEBUGMODE = debug;
    }

    public void setToken(String Token) {
        this.Token = Token;
    }

    /**
     * 初始化配置
     *
     * @param secretId  通讯密钥ID
     * @param secretKey 通讯密钥
     */
    public void init(String secretId, String secretKey) {
        this.secretId  = secretId;
        this.secretKey = secretKey;
    }

    public static String MD5(String input) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(input.getBytes());
            byte[]        messageDigest = digest.digest();
            StringBuilder hexString     = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2) h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 签名算法
     *
     * @param parameter 请求参数
     * @return 返回添加签名后的参数
     */
    private Parameter getParams(Parameter parameter) {
        Random random = new Random();
        int    nonce  = 100000 + random.nextInt(900000);
        // 取当前时间戳，精度：秒
        parameter.add("ts", System.currentTimeMillis() / 1000);
        // 取6位随机整数
        parameter.add("nonce", nonce);
        // 填充密钥ID
        parameter.add("secretId", this.secretId);
        // 排序并拼接字符串
        String tmp = parameter.toParameterString() + "&key=" + this.secretKey;
        // MD5后转大写，并写入签名
        parameter.add("sign", MD5(tmp));
        return parameter;
    }

    /**
     * 注册系统
     * 设备注册后返回一个Token，后续的所有请求都将使用该Token
     * 允许重复注册，这将返回一个全新的Token，旧的Token将会失效
     *
     * @param deviceName       设备名称，例如：HUAWEI-ATH-TL00H
     * @param deviceId         设备唯一标识，例如：8BN02179015937
     * @param carType          设备类型，例如：HUAWEI
     * @param responseCallback 执行回调
     * @link https://www.eolink.com/share/inside/9000c252d52ba71d961dc2cf46130e43/api/2942052/detail/54168105
     */
    public void register(String deviceName, String deviceId, String carType, final HttpResponseCallback responseCallback) {
        Parameter parameter = new Parameter();
        parameter.add("name", deviceName);
        parameter.add("UUID", deviceId);
        parameter.add("carType", carType);
        HttpRequest.GET(context, "/system/register", getParams(parameter), new JsonResponseListener() {
            @Override
            public void onResponse(JsonMap main, Exception error) {
                result(main, error, responseCallback);
            }
        });
    }

    /**
     * 注销系统
     * 清除Token，使得Token无效，但不会清除服务器上的该设备的数据
     * 因为没有Token，所以该设备之后的请求将无效
     *
     * @param deviceId         设备唯一标识
     * @param responseCallback 执行回调
     * @link https://www.eolink.com/share/inside/9000c252d52ba71d961dc2cf46130e43/api/2942052/detail/54168107
     */
    public void unregister(String deviceId, final HttpResponseCallback responseCallback) {
        Parameter parameter = new Parameter();
        parameter.add("UUID", deviceId);
        HttpRequest.GET(context, "/system/unregister", getParams(parameter), new JsonResponseListener() {
            @Override
            public void onResponse(JsonMap main, Exception error) {
                result(main, error, responseCallback);
            }
        });
    }

    /**
     * 根据文件名获取apk下载地址
     *
     * @param name             文件名
     * @param exact            文件名是否完整匹配: 0模糊,1精确
     * @param responseCallback 执行回调
     * @link https://www.eolink.com/share/inside/9000c252d52ba71d961dc2cf46130e43/api/2942052/detail/54167708
     */
    public void getAPKFile(String name, int exact, final HttpResponseCallback responseCallback) {
        Parameter parameter = new Parameter();
        parameter.add("name", name);
        parameter.add("exact", exact);

        HttpRequest.GET(context, "/data/apk", new Parameter().add("Token", Token), getParams(parameter), new JsonResponseListener() {
            @Override
            public void onResponse(JsonMap main, Exception error) {
                result(main, error, responseCallback);
            }
        });
    }

    /**
     * 获取APK文件列表
     *
     * @param name             文件名，不限制请传空值
     * @param exact            文件名是否完整匹配，此参数需name参数非空时有效
     * @param carType          适配车型，如需所有车型请传空值
     * @param page             数据分页，默认值为1
     * @param responseCallback 执行回调
     * @link https://www.eolink.com/share/inside/9000c252d52ba71d961dc2cf46130e43/api/2942052/detail/54168093
     */
    public void getAPKList(String name, int exact, String carType, int page, final HttpResponseCallback responseCallback) {
        Parameter parameter = new Parameter();
        parameter.add("name", name);
        parameter.add("exact", exact);
        parameter.add("carType", carType);
        parameter.add("page", page);

        HttpRequest.GET(context, "/data/list", new Parameter().add("Token", Token), getParams(parameter), new JsonResponseListener() {
            @Override
            public void onResponse(JsonMap main, Exception error) {
                result(main, error, responseCallback);
            }
        });
    }

    /**
     * 文件下载
     *
     * @param id                 文件ID
     * @param area               文件所处区域
     * @param file               存储文件对象
     * @param onDownloadListener 下载回调
     */
    public void download(String id, String area, File file, final OnDownloadListener onDownloadListener) {
        String      url         = "/apk/download?id=" + id + "&area=" + area + "&Token=" + Token;
        HttpRequest httpRequest = HttpRequest.build(context, url);
        httpRequest.doDownload(file, new com.kongzue.baseokhttp.listener.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(File file) {
                onDownloadListener.onDownloadSuccess(file);
            }

            @Override
            public void onDownloading(int progress) {
                onDownloadListener.onDownloading(progress);
            }

            @Override
            public void onDownloadFailed(Exception e) {
                onDownloadListener.onDownloadFailed(e);
            }
        });
    }

    /**
     * 上报日志
     *
     * @param data             日志数据内容
     * @param responseCallback 执行回调
     * @link https://www.eolink.com/share/inside/9000c252d52ba71d961dc2cf46130e43/api/2942052/detail/54168375
     */
    public void report(String data, final HttpResponseCallback responseCallback) {
        Parameter parameter = new Parameter();
        parameter.add("data", data);
        if (data.isEmpty()) {
            responseCallback.onFail("data is empty");
            return;
        }
        // 防止因data中的非法字符引起签名失败，故该接口无需签名
        HttpRequest.POST(context, "/apk/report", new Parameter().add("Token", Token), parameter, new JsonResponseListener() {
            @Override
            public void onResponse(JsonMap main, Exception error) {
                result(main, error, responseCallback);
            }
        });
    }

    /**
     * 通用处理返回回调
     *
     * @param result           JsonMap 对象, 详情请参见 https://github.com/kongzue/BaseJson 项目
     * @param error            若 error 不为空，则为请求失败，反之则成功
     * @param responseCallback 响应回调
     */
    private void result(JsonMap result, Exception error, HttpResponseCallback responseCallback) {
        if (error == null) {
            if (result.getInt("code") == 0) {
                responseCallback.onSuccess(result.getJsonMap("data"));
            } else {
                responseCallback.onFail(result.getString("msg"));
            }
        } else {
            responseCallback.onFail(error.getMessage());
        }
    }
}
