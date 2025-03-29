package com.example.clouddemo.api;

import com.example.clouddemo.model.ResponseData;
import com.example.clouddemo.utils.Utils;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class ApiManager {
    private final String TOKEN_PREFIX;
    private final CloudinaryService apiService;

    public ApiManager() {
        TOKEN_PREFIX = "Bearer ";
        apiService = RetrofitClient.getInstance().getService();
    }

    public void getSignatur(String token, Map<String, Object> config, Callback<ResponseData<Object>> callback) {
        String tokenWithPrefix = Utils.formatToken(token);
        String configUrl = Utils.getConfigUrl(config);
        Call<ResponseData<Object>> call = apiService.getSignatur(tokenWithPrefix, configUrl);
        call.enqueue(callback);
    }
}