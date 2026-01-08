package com.example.testing.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testing.data.remote.api.ApiClient;
import com.example.testing.data.remote.api.ApiService;
import com.example.testing.data.remote.response.Model;
import com.example.testing.data.remote.response.ModelResponse;

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

    public interface RefreshCallback {
        void onResult(boolean isSuccess);
    }

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
            fetchModelsFromApi(null);
        } else {
            // Return immediately if cached
            modelsLiveData.setValue(cachedModels);
        }
        return modelsLiveData;
    }

    public void refreshModels() {
        fetchModelsFromApi(null);
    }

    public void refreshModels(RefreshCallback callback) {
        fetchModelsFromApi(callback);
    }

    private void fetchModelsFromApi(RefreshCallback callback) {
        // We don't need an API key for the public models endpoint in many cases,
        // but usually OpenRouter allows public access to /models without auth.
        // If auth is required in future, pass it here.

        apiService.getModels().enqueue(new Callback<ModelResponse>() {
            @Override
            public void onResponse(Call<ModelResponse> call, Response<ModelResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedModels = response.body().getData();
                    modelsLiveData.postValue(cachedModels);
                    if (callback != null) {
                        callback.onResult(true);
                    }
                } else {
                    if (callback != null) {
                        callback.onResult(false);
                    }
                }
            }

            @Override
            public void onFailure(Call<ModelResponse> call, Throwable t) {
                // Handle error if needed, or just leave LiveData empty
                t.printStackTrace();
                if (callback != null) {
                    callback.onResult(false);
                }
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

    // --- ADDED: Check if we already have models ---
    public boolean isModelsCached() {
        return cachedModels != null && !cachedModels.isEmpty();
    }
}