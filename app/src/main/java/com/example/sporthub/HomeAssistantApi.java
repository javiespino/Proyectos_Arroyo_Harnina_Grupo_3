package com.example.sporthub;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HomeAssistantApi {

    private static final OkHttpClient client = new OkHttpClient();

    private static final String BASE_URL = "http://suarezdefigueroa.es:8086";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkNjI1MzI1MGRkN2I0OTUwYjMwMzEwMWE4NjJiOTYwOCIsImlhdCI6MTc2MDg5MjExNiwiZXhwIjoyMDc2MjUyMTE2fQ.PCFqgLj880O5hA5ztGO6ON-pGXF1-H3rDWgG3qAtioY";

    public interface SensorCallback {
        void onSuccess(JSONObject json);
        void onError(IOException e, int code);
    }

    public static void getSensorState(String entityId, SensorCallback cb) {
        String url = BASE_URL + "/api/states/" + entityId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + TOKEN)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                cb.onError(e, -1);
            }

            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        cb.onError(new IOException("HTTP " + response.code()), response.code());
                        return;
                    }

                    JSONObject json = new JSONObject(body.string());
                    cb.onSuccess(json);

                } catch (Exception ex) {
                    cb.onError(new IOException(ex), response.code());
                }
            }
        });
    }
}
