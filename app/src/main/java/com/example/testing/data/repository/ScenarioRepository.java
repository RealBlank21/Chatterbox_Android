package com.example.testing.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;

import com.example.testing.data.local.AppDatabase;
import com.example.testing.data.local.entity.Scenario;
import com.example.testing.data.local.dao.ScenarioDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScenarioRepository {

    private static volatile ScenarioRepository INSTANCE;
    private final ScenarioDao scenarioDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private ScenarioRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.scenarioDao = db.scenarioDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static ScenarioRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (ScenarioRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ScenarioRepository(application);
                }
            }
        }
        return INSTANCE;
    }

    public void insert(Scenario scenario) {
        executorService.execute(() -> {
            if (scenario.isDefault()) {
                scenarioDao.clearDefaultsForCharacter(scenario.getCharacterId());
            }
            scenarioDao.insert(scenario);
        });
    }

    public void update(Scenario scenario) {
        executorService.execute(() -> {
            if (scenario.isDefault()) {
                scenarioDao.clearDefaultsForCharacter(scenario.getCharacterId());
            }
            scenarioDao.update(scenario);
        });
    }

    public void delete(Scenario scenario) {
        executorService.execute(() -> scenarioDao.delete(scenario));
    }

    public LiveData<List<Scenario>> getScenariosForCharacter(int characterId) {
        return scenarioDao.getScenariosForCharacter(characterId);
    }

    public Scenario getDefaultScenarioSync(int characterId) {
        return scenarioDao.getDefaultScenario(characterId);
    }

    public LiveData<Scenario> getDefaultScenarioLive(int characterId) {
        return scenarioDao.getDefaultScenarioLive(characterId);
    }

    public Scenario getScenarioByIdSync(int id) {
        return scenarioDao.getScenarioById(id);
    }

    public LiveData<Scenario> getScenarioByIdLive(int id) {
        return scenarioDao.getScenarioByIdLive(id);
    }
}