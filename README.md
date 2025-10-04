# Chatterbox Android

## 🌟 Project Overview

Chatterbox is an Android application designed to allow users to interact with customizable AI characters. It uses the OpenRouter API for large language model (LLM) communication, providing a simplistic platform for creating and managing character-driven conversations.

The application is built using modern Android architecture components, including Room for local persistence and LiveData/ViewModel for data management.

## ✨ Features

* **Customizable AI Characters:** Create and manage unique AI characters with custom names, personalities (system prompts), default models, and initial greeting messages.
* **Per-Character Model Parameters:** Set model-specific parameters like `temperature` and `max_tokens` for individual characters.
* **User Settings:** Configure a global username, default API key, preferred model, and an optional global system prompt.
* **Conversation History:** View and resume past conversations from a dedicated history screen.
* **Message Regeneration:** Long-press on an AI message in the chat to select an option to regenerate the response.
* **Markdown Support:** Chat messages are rendered to display markdown formatting, including tables.

## 🛠️ Technology Stack

* **Platform:** Android (Java)
* **Minimum SDK:** 33
* **Architecture:** Android Jetpack (ViewModel, LiveData)
* **Database:** Room Persistence Library
* **Networking:** Retrofit and Gson for API communication
* **Image Loading:** Glide for efficient image handling (character profiles)
* **Markdown Rendering:** Markwon (with Tables Extension)
* **AI Backend:** OpenRouter API.

## ⚙️ Installation and Setup

### Prerequisites

1.  Android Studio (Dolphin or newer recommended).
2.  Java Development Kit (JDK) 11 or higher.
3.  An [OpenRouter](https://openrouter.ai/) API Key.

### Steps

1.  **Clone the repository:**
    ```bash
    git clone github.com/RealBlank21/Chatterbox_Android chatterbox_android
    ```
2.  **Open in Android Studio:**
    Open the cloned `chatterbox_android` directory in Android Studio.
3.  **Sync and Build:**
    Allow Gradle to sync and download all necessary dependencies.

## 🔑 Configuration (API Key)

The application requires an API key to communicate with the LLMs.

1.  On the main Character list screen, tap the **Settings** option in the overflow menu (three vertical dots).
2.  Enter your **OpenRouter API Key** into the corresponding field.
3.  (Optional) Set a `Preferred Model` and `Global System Prompt`.
4.  Tap **Save**.

The application will now be able to fetch responses from the AI.
