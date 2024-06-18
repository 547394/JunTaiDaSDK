package com.jianxunfuture.juntaidasdk;

import com.kongzue.baseokhttp.util.JsonMap;

public interface HttpResponseCallback {

    void onSuccess(JsonMap result);

    // 失败
    void onFail(String error);
}
