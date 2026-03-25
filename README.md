# Mindustry Chat Translator

A Mindustry mod that automatically translates chat messages in real-time, supporting multiple translation engines including Google, Bing, and OpenAI.

[//]: # (![Settings Interface]&#40;./README.png&#41;)

## Features

- 🌍 **Real-time Translation**: Automatically translates incoming chat messages to your language
- 🔧 **Multiple Translation Engines**: Choose between Google, Bing, or OpenAI (ChatGPT)
- 🎨 **Customizable Display**: Options to hide original messages and preserve player name colors
- 🖥️ **Server Message Support**: Optionally translate server announcements and system messages
- 🌐 **Multi-language UI**: English, Simplified Chinese (简体中文), and Traditional Chinese (繁體中文)
- ⚙️ **Advanced OpenAI Configuration**: Fine-tune temperature, custom prompts, and model selection

## Translation Engines

### Google Translate
Free, fast, and reliable for general use.

### Bing Translator
Free, good quality, and stable. **Default choice**.

### OpenAI (ChatGPT)
Best quality with context-aware translation. Requires an API key from [OpenAI Platform](https://platform.openai.com/).

## Configuration

Access settings through: **Settings** → **Translator**

Configure translation engine, display options, and OpenAI parameters to suit your needs.

## How It Works

1. Listens to incoming chat messages
2. Automatically detects source language
3. Translates to your game language
4. Displays translated text in chat

**Note**: Your own messages are never translated.

## Building from Source

### Prerequisites
- JDK 17 or higher

### Build Commands

Desktop testing:
```bash
./gradlew jar
```

Android compatible:
```bash
./gradlew deploy
```

## Contributing

Contributions are welcome! Feel free to:
- Report bugs via Issues
- Submit feature requests
- Create pull requests
- Add new language translations in `bundles/` directory

## Privacy Notice

Messages are sent to external translation services (Google, Bing, or OpenAI) for processing. No data is stored locally beyond settings.

---

**Note**: This is an unofficial mod and is not affiliated with or endorsed by the official Mindustry development team.
