package com.example.clouddemo.utils;

import android.util.Log;

import com.example.clouddemo.model.ResponseData;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public class Utils {
    // Format token with prefix if needed
    public static String formatToken(String token) {
        String TOKEN_PREFIX = "Bearer ";
        if (token != null && !token.startsWith(TOKEN_PREFIX)) {
            return TOKEN_PREFIX + token;
        }
        return token;
    }

    // convert config map to config url
    public static String getConfigUrl(Map<String, Object> config) {
        StringBuilder configUrl = new StringBuilder();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (configUrl.length() > 0) {
                configUrl.append("&");
            }
            configUrl.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return configUrl.toString();
    }

    // get data filed data body
    public static String getDataBody(ResponseData<Object> responseData, String field) {
        try {
            Gson gson = new Gson();
            JsonElement jsonElement = gson.toJsonTree(responseData.getData());
            JsonObject dataObject = jsonElement.getAsJsonObject();

            return dataObject.get(field).getAsString();
        } catch (Exception e) {
            Log.e("GetDataBody", "Error parsing response data");
            return "";
        }
    }
}
