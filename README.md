# NeuroGate - AI Misuse Detection App

## 🚀 Hackathon Project: Detecting and Mitigating AI Misuse in Real Time

NeuroGate is an Android application that provides real-time monitoring and detection of AI misuse, specifically focusing on deepfake creation, celebrity impersonation, and other harmful AI-generated content.

## ✨ Features

### 🔍 Real-Time AI Misuse Detection
- **Multi-AI Service Integration**: Uses OpenAI Content Moderation, Google Perspective API, and Azure Content Moderator
- **Context-Aware Analysis**: Advanced AI models understand context and intent
- **Pattern Recognition**: Detects deepfake requests, celebrity impersonation, harmful content, and copyright violations
- **Confidence Scoring**: Provides confidence levels for each detection
- **Fallback System**: Local analysis when external APIs are unavailable

### 🚨 Floating Alert System
- **Instant Notifications**: Floating alerts appear when misuse is detected
- **Color-Coded Categories**: Different colors for different types of misuse
- **Actionable Suggestions**: Provides helpful suggestions to users

### 🧹 Automatic Input Clearing
- **Smart Clearing**: Automatically clears input fields when misuse is detected
- **Configurable**: Can be enabled/disabled in settings

### 📊 Dashboard & Analytics
- **Flagged Interactions**: View all detected misuse attempts
- **Statistics**: Track different types of misuse over time
- **User Actions**: Record how users respond to detections
- **Export Capabilities**: Export flagged interactions for analysis

### ⚙️ Configurable Settings
- **Sensitivity Levels**: Adjust detection sensitivity (Low, Medium, High)
- **Real-time Monitoring**: Enable/disable real-time detection
- **Alert Preferences**: Customize floating alert behavior

## 🔧 Language Model Integration

NeuroGate now uses **real Language Models** for dynamic, context-aware detection instead of static patterns!

### 🤖 Supported LLM Services:

### 1. **Hugging Face Transformers** 🤗
- Get your API key from [Hugging Face](https://huggingface.co/settings/tokens)
- Uses BERT-based models for classification
- Update `HUGGINGFACE_API_KEY` in `app/build.gradle.kts`

### 2. **OpenAI GPT** 🧠
- Get your API key from [OpenAI Platform](https://platform.openai.com/api-keys)
- Uses GPT-3.5-turbo for context understanding
- Update `OPENAI_API_KEY` in `app/build.gradle.kts`

### 3. **Cohere AI** 💫
- Get your API key from [Cohere Dashboard](https://dashboard.cohere.ai/)
- Uses classification models for semantic analysis
- Update `COHERE_API_KEY` in `app/build.gradle.kts`

### 4. **Google Perspective API** 🔍
- Get your API key from [Google Cloud Console](https://console.cloud.google.com/)
- Used for toxicity and threat detection
- Update `PERSPECTIVE_API_KEY` in `app/build.gradle.kts`

### 5. **Azure Content Moderator** 🛡️
- Get your API key from [Azure Portal](https://portal.azure.com/)
- Used for additional content moderation
- Update `AZURE_CONTENT_MODERATOR_KEY` in `app/build.gradle.kts`

### 6. **Anthropic Claude** 🎭
- Get your API key from [Anthropic Console](https://console.anthropic.com/)
- Advanced reasoning capabilities
- Update `ANTHROPIC_API_KEY` in `app/build.gradle.kts`

### Configuration Example:
```kotlin
// Language Model APIs
buildConfigField("String", "HUGGINGFACE_API_KEY", "\"hf_your-actual-token\"")
buildConfigField("String", "OPENAI_API_KEY", "\"sk-your-actual-openai-key\"")
buildConfigField("String", "COHERE_API_KEY", "\"your-actual-cohere-key\"")
buildConfigField("String", "ANTHROPIC_API_KEY", "\"your-actual-anthropic-key\"")

// Additional APIs
buildConfigField("String", "PERSPECTIVE_API_KEY", "\"your-actual-perspective-key\"")
buildConfigField("String", "AZURE_CONTENT_MODERATOR_KEY", "\"your-actual-azure-key\"")
```

### 🎯 **How LLM Detection Works:**

1. **Parallel Processing**: Multiple LLMs analyze simultaneously
2. **Ensemble Voting**: Results are combined using majority vote
3. **Context Understanding**: Real language models understand intent and context
4. **Flexible Analysis**: No rigid patterns - adapts to new variations
5. **Fallback System**: Enhanced local analysis when APIs unavailable

**Note**: If no API keys are configured, the app uses an enhanced fallback system with improved pattern matching.

## 🛠️ Technical Architecture

### Core Components

1. **Data Layer**
   - `AIMisuseModels.kt`: Data models for API requests/responses
   - `AIService.kt`: Real AI service with OpenAI, Perspective, and Azure integration
   - `AIRepository.kt`: Repository pattern for data management

2. **AI Services**
   - **RealAIService**: Integrates multiple AI APIs for comprehensive analysis
   - **OpenAI Content Moderation**: Detects harmful, sexual, violent, and inappropriate content
   - **Google Perspective API**: Analyzes toxicity, threats, and identity attacks
   - **Azure Content Moderator**: Additional content policy enforcement
   - **Fallback Analysis**: Improved local pattern matching when APIs unavailable

3. **UI Layer**
   - `InputScreen.kt`: Main input interface with real-time detection
   - `DashboardScreen.kt`: Analytics and flagged interactions
   - `SettingsScreen.kt`: App configuration
   - `FloatingAlert.kt`: Animated alert component

4. **Business Logic**
   - `AIDetectionViewModel.kt`: ViewModel with debounced analysis
   - `AIDetectionViewModelFactory.kt`: Dependency injection for context

### AI Detection Flow

1. **User Input**: Text is entered in real-time
2. **Debouncing**: 300ms delay to avoid excessive API calls
3. **Parallel Analysis**: Multiple AI services analyze simultaneously
4. **Ensemble Scoring**: Results combined with weighted scoring
5. **Decision Making**: Higher threshold (0.6) for more accuracy
6. **User Feedback**: Floating alerts and suggestions

### Detection Categories

- **DEEPFAKE**: Face replacement and manipulation requests
- **CELEBRITY_IMPERSONATION**: Impersonating public figures
- **HARMFUL_CONTENT**: Violence, sexual content, hate speech
- **COPYRIGHT_VIOLATION**: Using copyrighted characters/brands
- **PRIVACY_VIOLATION**: Personal data and privacy concerns
- **NONE**: Safe content for AI generation

## 🚀 Getting Started

1. **Clone the repository**
2. **Configure API keys** in `app/build.gradle.kts`
3. **Build and run** the application
4. **Test with various prompts** to see the detection in action

## 📱 Usage Examples

### Safe Prompts (Should NOT trigger alerts):
- "Generate a cute cat picture"
- "Create a beautiful landscape"
- "Design a logo for my business"
- "Make an educational diagram"

### Misuse Prompts (Should trigger alerts):
- "Replace my face with Tom Cruise"
- "Make me look like Brad Pitt"
- "Create a deepfake video"
- "Generate nude content"

## 🔒 Privacy & Security

- All API calls use HTTPS
- API keys are stored securely in BuildConfig
- No user data is stored permanently
- Local analysis available when offline

## 🤝 Contributing

This is a hackathon project. Feel free to:
- Report bugs and issues
- Suggest improvements
- Add new detection patterns
- Enhance the UI/UX

## 📄 License

This project is created for educational and hackathon purposes.
