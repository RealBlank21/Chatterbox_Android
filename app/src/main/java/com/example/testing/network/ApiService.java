package com.example.testing.network;

import com.example.testing.network.response.ChatCompletionResponse; // Import the new class
import com.example.testing.network.response.ModelResponse;
import com.example.testing.network.request.ApiRequest;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    @POST("v1/chat/completions")
    Call<ChatCompletionResponse> getChatCompletion( // Changed from ApiResponse
                                                    @Header("Authorization") String authToken,
                                                    @Body ApiRequest requestBody
    );

    @GET("v1/credits")
    Call<ResponseBody> getCredits(@Header("Authorization") String authToken);

    @GET("v1/models")
    Call<ModelResponse> getModels();
}