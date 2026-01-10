package com.example.testing.ui.settings.utils;

import android.view.View;
import android.widget.TextView;
import com.example.testing.data.remote.api.ApiClient;
import com.example.testing.data.remote.api.ApiService;
import org.json.JSONObject;
import java.text.DecimalFormat;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreditsManager {

    public static void fetchCredits(String apiKey, TextView textViewCredits) {
        textViewCredits.setVisibility(View.VISIBLE);
        textViewCredits.setText("Loading credits...");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCredits("Bearer " + apiKey).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonString);
                        JSONObject data = jsonObject.optJSONObject("data");

                        if (data != null) {
                            double totalCredits = data.optDouble("total_credits", 0.0);
                            double totalUsage = data.optDouble("total_usage", 0.0);
                            double remainingCredits = totalCredits - totalUsage;

                            DecimalFormat df = new DecimalFormat("#,##0.00");
                            String formattedCredits = df.format(remainingCredits);

                            textViewCredits.post(() -> textViewCredits.setText("Remaining Credits: $" + formattedCredits));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        textViewCredits.post(() -> textViewCredits.setText("Error reading credits"));
                    }
                } else {
                    textViewCredits.post(() -> textViewCredits.setText("Failed to load credits"));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                textViewCredits.post(() -> textViewCredits.setText("Network error"));
            }
        });
    }
}