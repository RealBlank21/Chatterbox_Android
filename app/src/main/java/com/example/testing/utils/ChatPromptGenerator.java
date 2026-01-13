package com.example.testing.utils;

import android.text.TextUtils;

import com.example.testing.data.local.dao.PersonaDao;
import com.example.testing.data.local.entity.Character;
import com.example.testing.data.local.entity.Conversation;
import com.example.testing.data.local.entity.Message;
import com.example.testing.data.local.entity.Persona;
import com.example.testing.data.local.entity.Scenario;
import com.example.testing.data.local.entity.User;
import com.example.testing.data.repository.ScenarioRepository;
import com.example.testing.data.remote.request.RequestMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatPromptGenerator {

    private final PersonaDao personaDao;
    private final ScenarioRepository scenarioRepository;
    private final SimpleDateFormat dayFormatter;
    private final SimpleDateFormat timeFormatter;

    public ChatPromptGenerator(PersonaDao personaDao, ScenarioRepository scenarioRepository) {
        this.personaDao = personaDao;
        this.scenarioRepository = scenarioRepository;
        this.dayFormatter = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        this.timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    public List<RequestMessage> buildApiRequestMessages(Conversation conversation, User user, Character character, List<Message> messageHistory) {
        List<RequestMessage> requestMessages = new ArrayList<>();

        long creationTimestamp = !messageHistory.isEmpty() ? messageHistory.get(0).getTimestamp() : System.currentTimeMillis();
        Date creationDate = new Date(creationTimestamp);
        String formattedDay = dayFormatter.format(creationDate);
        String formattedTime = timeFormatter.format(creationDate);

        int limit = character.getContextLimit() != null && character.getContextLimit() > 0
                ? character.getContextLimit() : user.getDefaultContextLimit();

        List<Message> messagesToSend = messageHistory;
        if (limit > 0) {
            int keepCount = limit * 2;
            if (messageHistory.size() > keepCount) {
                messagesToSend = messageHistory.subList(messageHistory.size() - keepCount, messageHistory.size());
            }
        }

        String characterName = character.getName() != null ? character.getName() : "Character";
        String userName = "User";

        String globalPrompt = user.getGlobalSystemPrompt() != null ? user.getGlobalSystemPrompt() : "";
        String characterPersonality = character.getPersonality() != null ? character.getPersonality() : "";

        int personaIdToUse = user.getCurrentPersonaId();
        if (conversation != null && conversation.getPersonaId() != null) {
            personaIdToUse = conversation.getPersonaId();
        }

        StringBuilder personaPromptBuilder = new StringBuilder();
        if (personaIdToUse != -1) {
            Persona persona = personaDao.getPersonaById(personaIdToUse);
            if (persona != null) {
                if (persona.getName() != null && !persona.getName().isEmpty()) {
                    userName = persona.getName();
                }
                personaPromptBuilder.append("User Persona:\n");
                if (persona.getName() != null && !persona.getName().isEmpty()) {
                    personaPromptBuilder.append("Name: ").append(persona.getName()).append("\n");
                }
                if (persona.getDescription() != null && !persona.getDescription().isEmpty()) {
                    personaPromptBuilder.append("Description: ").append(persona.getDescription()).append("\n");
                }
                personaPromptBuilder.append("\n");
            }
        }
        String personaPrompt = personaPromptBuilder.toString();

        StringBuilder scenarioPromptBuilder = new StringBuilder();
        if (conversation != null && conversation.getScenarioId() != null) {
            Scenario selectedScenario = scenarioRepository.getScenarioByIdSync(conversation.getScenarioId());
            if (selectedScenario != null) {
                scenarioPromptBuilder.append("Scenario Context:\n");
                if (!TextUtils.isEmpty(selectedScenario.getName())) {
                    scenarioPromptBuilder.append("Scenario: ").append(selectedScenario.getName()).append("\n");
                }
                scenarioPromptBuilder.append(selectedScenario.getDescription()).append("\n\n");
            }
        } else {
            if (!TextUtils.isEmpty(character.getDefaultScenario())) {
                scenarioPromptBuilder.append("Scenario Context:\n");
                scenarioPromptBuilder.append(character.getDefaultScenario()).append("\n\n");
            }
        }
        String scenarioPrompt = scenarioPromptBuilder.toString();

        globalPrompt = replacePlaceholders(globalPrompt, userName, characterName);
        characterPersonality = replacePlaceholders(characterPersonality, userName, characterName);
        personaPrompt = replacePlaceholders(personaPrompt, userName, characterName);
        scenarioPrompt = replacePlaceholders(scenarioPrompt, userName, characterName);

        globalPrompt = globalPrompt.replace("{day}", formattedDay).replace("{time}", formattedTime);
        characterPersonality = characterPersonality.replace("{day}", formattedDay).replace("{time}", formattedTime);

        String finalSystemPrompt = globalPrompt + "\n" + personaPrompt + characterPersonality + "\n" + scenarioPrompt;

        if (character.isTimeAware()) {
            finalSystemPrompt += "\nThis conversation was started on " + formattedDay + " at " + formattedTime + ".";
        }
        if (!TextUtils.isEmpty(finalSystemPrompt.trim())) {
            requestMessages.add(new RequestMessage("system", finalSystemPrompt.trim()));
        }

        for (Message msg : messagesToSend) {
            if (character.isTimeAware() && "user".equals(msg.getRole())) {
                Date msgDate = new Date(msg.getTimestamp());
                String msgTime = dayFormatter.format(msgDate) + " at " + timeFormatter.format(msgDate);
                requestMessages.add(new RequestMessage("system", "Current time: " + msgTime));
            }
            String content = replacePlaceholders(msg.getContent(), userName, characterName);
            requestMessages.add(new RequestMessage(msg.getRole(), content));
        }

        if (!messagesToSend.isEmpty()) {
            Message lastMessage = messagesToSend.get(messagesToSend.size() - 1);
            if ("assistant".equals(lastMessage.getRole()) && "length".equals(lastMessage.getFinishReason())) {
                requestMessages.add(new RequestMessage("system", "Continue from where you stopped, continue with the next logical action."));
            }
        }

        return requestMessages;
    }

    private String replacePlaceholders(String text, String userName, String characterName) {
        if (text == null) return "";
        return text.replace("{{user}}", userName)
                .replace("{{character}}", characterName);
    }
}