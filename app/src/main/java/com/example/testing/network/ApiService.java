package com.example.testing.network;

import com.example.testing.network.response.ApiResponse;
import com.example.testing.network.response.ModelResponse;
import com.example.testing.network.request.ApiRequest;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Streaming; // Import this

public interface ApiService {

    @Streaming // Add this annotation
    @POST("v1/chat/completions")
    Call<ResponseBody> getChatCompletionStream( // Changed return type and name
                                                @Header("Authorization") String authToken,
                                                @Body ApiRequest requestBody
    );

    @GET("v1/credits")
    Call<ResponseBody> getCredits(
            @Header("Authorization") String authToken
    );

    @GET("v1/models")
    Call<ModelResponse> getModels();
}