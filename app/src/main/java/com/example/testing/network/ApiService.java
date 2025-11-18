package com.example.testing.network;

import com.example.testing.network.response.ApiResponse;
import com.example.testing.network.request.ApiRequest;
// Add these imports
import okhttp3.ResponseBody;
import retrofit2.http.GET;
// ------------------
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {
    @POST("v1/chat/completions")
    Call<ApiResponse> getChatCompletion(
            @Header("Authorization") String authToken,
            @Body ApiRequest requestBody
    );

    // Add this method
    @GET("v1/credits")
    Call<ResponseBody> getCredits(
            @Header("Authorization") String authToken
    );
}