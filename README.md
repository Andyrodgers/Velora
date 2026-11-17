# Velora

**Velora** is a comprehensive menstrual health and wellness companion app designed to empower users with deep insights into their bodies. It combines advanced cycle tracking with AI-driven guidance to provide a holistic view of reproductive health, mental well-being, and lifestyle.

## 🚀 Key Features

*   **Intelligent Cycle Tracking:** Precise tracking and prediction of periods, ovulation, and fertile windows based on historical data and user-logged symptoms.
*   **Vela AI Health Companion:** A compassionate AI assistant integrated into the app. It provides instant, evidence-based answers to health queries using both a local knowledge engine and the Gemini AI model.
*   **Holistic Health Logging:** Track physical symptoms, moods, energy levels, and libido to understand how your hormones affect your daily life.
*   **PCOS Assessment:** A dedicated module to monitor and assess symptoms related to Polycystic Ovary Syndrome, facilitating better clinical discussions with healthcare providers.
*   **Partner Synchronization:** Securely share cycle status with a partner to enhance communication and mutual understanding.
*   **Community Support:** An anonymous "Secret Chat" feature for community interaction and shared experiences.
*   **Visual Trends:** Custom interactive graphs and cycle views (TrendGraphView, CycleCircleView) to visualize health patterns over time.

## 🛠️ Technology Stack

*   **Languages:** Kotlin & Java
*   **Architecture:** MVVM (Model-View-ViewModel) with ViewBinding
*   **Backend:** [Supabase](https://supabase.com/) (Auth, Database, Real-time, Storage)
*   **Local Database:** Room Persistence Library
*   **AI Models:** Google Gemini AI & OpenRouter (GLM-5-Turbo)
*   **Networking:** OkHttp & Ktor
*   **Background Tasks:** WorkManager (for reminders and data sync)
*   **Image Loading:** Glide

## 📦 Installation & Setup

To run this project locally, you will need to set up your own API keys.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Andyrodgers/Velora.git
    ```
2.  **Configure API Keys:**
    Create a `local.properties` file in the root directory (if it doesn't exist) and add your keys:
    ```properties
    OPENROUTER_API_KEY=your_openrouter_api_key
    SUPABASE_URL=your_supabase_project_url
    SUPABASE_KEY=your_supabase_anon_key
    ```
3.  **Firebase/Google Services:**
    Place your `google-services.json` file in the `app/` directory.
4.  **Build & Run:**
    Open the project in Android Studio, sync Gradle, and run it on an emulator or physical device.

## 🔒 Security

This project is configured to keep sensitive information secure. API keys are managed via `local.properties` and injected into the build using `BuildConfig`, ensuring they are never committed to the Git history.

## 📄 License

This project is for educational/personal use. (Add your preferred license here, e.g., MIT).
