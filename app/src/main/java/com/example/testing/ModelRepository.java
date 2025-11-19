package com.example.testing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testing.network.ApiClient;
import com.example.testing.network.ApiService;
import com.example.testing.network.response.Model;
import com.example.testing.network.response.ModelResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModelRepository {

    private static ModelRepository instance;
    private final ApiService apiService;

    // Cache the models in memory
    private List<Model> cachedModels = null;
    private final MutableLiveData<List<Model>> modelsLiveData = new MutableLiveData<>();

    private ModelRepository() {
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    public static synchronized ModelRepository getInstance() {
        if (instance == null) {
            instance = new ModelRepository();
        }
        return instance;
    }

    public LiveData<List<Model>> getModels() {
        if (cachedModels == null) {
            fetchModelsFromApi();
        } else {
            // Return immediately if cached
            modelsLiveData.setValue(cachedModels);
        }
        return modelsLiveData;
    }

    public void refreshModels() {
        fetchModelsFromApi();
    }

    private void fetchModelsFromApi() {
        // We don't need an API key for the public models endpoint in many cases,
        // but usually OpenRouter allows public access to /models without auth.
        // If auth is required in future, pass it here.

        apiService.getModels().enqueue(new Callback<ModelResponse>() {
            @Override
            public void onResponse(Call<ModelResponse> call, Response<ModelResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedModels = response.body().getData();
                    modelsLiveData.postValue(cachedModels);
                }
            }

            @Override
            public void onFailure(Call<ModelResponse> call, Throwable t) {
                // Handle error if needed, or just leave LiveData empty
                t.printStackTrace();
            }
        });
    }

    // Helper to find a model object by its ID string (for displaying details)
    public Model getModelById(String id) {
        if (cachedModels == null) return null;
        for (Model m : cachedModels) {
            if (m.getId().equals(id)) return m;
        }
        return null;
    }
}