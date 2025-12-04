<div align="center">

# 🎨 KToon

**Efficient serialization for Kotlin Multiplatform**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.0-4285F4?style=flat&logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web-lightgrey.svg)](https://kotlinlang.org/docs/multiplatform.html)

*Cut your API payload sizes by 30-60% without changing your code*

[Features](#-features) • [Quick Start](#-quick-start) • [Demo Apps](#-demo-apps) • [Documentation](#-documentation) • [Roadmap](#-roadmap)

</div>

---

## 🎯 What is KToon?

KToon is a Kotlin Multiplatform serialization library implementing the **TOON format** (Token-Oriented Object Notation). Think of it as JSON's efficient cousin—perfect for mobile apps, IoT devices, and anywhere bandwidth matters.

**JSON (553 bytes):**
```json
[
  {"id": 1, "name": "Alice", "email": "alice@example.com", "age": 30},
  {"id": 2, "name": "Bob", "email": "bob@example.com", "age": 25},
  {"id": 3, "name": "Charlie", "email": "charlie@example.com", "age": 35}
]
```

**TOON (179 bytes - 67% smaller!):**
```
users[3]{id,name,email,age}:
  1,Alice,alice@example.com,30
  2,Bob,bob@example.com,25
  3,Charlie,charlie@example.com,35
```

## ✨ Features

- 🚀 **30-60% smaller payloads** - Less data = faster loading
- 📱 **True multiplatform** - Android, iOS, Desktop, Web (JS + WASM)
- 🔌 **Drop-in replacement** - Works with your existing `@Serializable` classes
- ⚡ **Fast** - Single-pass O(N) serialization
- 🎯 **Type-safe** - Full kotlinx.serialization integration
- 🛠️ **Ktor ready** - Client and server ContentNegotiation support
- 🧩 **Minimal** - ~1000 lines of pure Kotlin, zero platform-specific code

## 🚀 Quick Start

### Installation

> **⚠️ TODO:** Publish to Maven Central

For now, clone and include as a local module:

```bash
git clone https://github.com/JosephSanjaya/ktoon.git
```

Add to your `settings.gradle.kts`:
```kotlin
include(:<"path/to/ktoon">)
```

### Basic Usage

```kotlin
import io.ktoon.Toon
import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Int, val name: String, val email: String)

fun main() {
    val users = listOf(
        User(1, "Alice", "alice@example.com"),
        User(2, "Bob", "bob@example.com")
    )
    
    // Serialize to TOON
    val toon = Toon.encodeToString(users)
    println(toon)
    // Output:
    // users[2]{id,name,email}:
    //   1,Alice,alice@example.com
    //   2,Bob,bob@example.com
    
    // Deserialize from TOON
    val decoded = Toon.decodeFromString<List<User>>(toon)
    println(decoded) // [User(1, Alice, alice@example.com), User(2, Bob, bob@example.com)]
}
```

### Ktor Client Integration

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktoon.ktor.toon

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()  // Fallback to JSON
        toon()  // Add TOON support
    }
}

// Automatic serialization/deserialization
val users = client.get("https://api.example.com/users") {
    headers {
        append("Accept", "application/toon")
    }
}.body<List<User>>()
```

### Ktor Server Integration

```kotlin
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktoon.ktor.server.toon

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()  // For application/json
            toon()  // For application/toon
        }
        
        routing {
            get("/users") {
                val users = listOf(
                    User(1, "Alice", "alice@example.com"),
                    User(2, "Bob", "bob@example.com")
                )
                call.respond(users) // Automatically uses TOON if Accept: application/toon
            }
        }
    }.start(wait = true)
}
```

## 🎮 Demo Apps

This repository includes two demo applications showcasing KToon's versatility:

### 1. API Demo App (Network-focused)
Real-time format comparison with live backend server.

```bash
# Start the backend server
./gradlew :backend:run

# Run the Compose Multiplatform app
./gradlew :composeApp:run

# Or test with curl
curl -H "Accept: application/json" http://localhost:8080/users
curl -H "Accept: application/toon" http://localhost:8080/users
```

**Features:**
- Side-by-side JSON vs TOON comparison
- Real-time byte count and savings metrics
- Interactive API testing
- Content negotiation demonstration

### 2. Offline Data Manager (Coming Soon)
Local-first architecture with efficient data storage.

**Features:**
- Efficient local data storage using TOON
- Batch sync operations
- Configuration file management
- Seed data bundling

## 📚 Documentation

- [ABOUT.md](ABOUT.md) - Project story and technical deep-dive
- [ktoon-core/README.md](ktoon-core/README.md) - Core serialization engine
- [ktoon-ktor/README.md](ktoon-ktor/README.md) - Ktor client integration
- [ktoon-ktor/EXAMPLES.md](ktoon-ktor/EXAMPLES.md) - Usage examples
- [ktoon-ktor-server/README.md](ktoon-ktor-server/README.md) - Ktor server integration
- [backend/README.md](backend/README.md) - Demo server setup

## 🏗️ Project Structure

```
ktoon/
├── ktoon-core/              # Core serialization engine (~1000 lines)
│   ├── Toon.kt             # Public API
│   ├── ToonEncoder.kt      # Serialization logic
│   ├── ToonDecoder.kt      # Deserialization logic
│   └── ToonLexer.kt        # Tokenization
├── ktoon-ktor/             # Ktor client integration
├── ktoon-ktor-server/      # Ktor server integration
├── backend/                # Demo backend server
└── composeApp/             # Demo Compose Multiplatform app
```

## 🛣️ Roadmap

### v0.3.0 - More Integrations
- [ ] **Retrofit support** - ContentConverter for Retrofit
- [ ] OkHttp interceptor

### v1.0.0 - Production Ready
- [ ] **Maven Central publication**
- [ ] Comprehensive documentation site
- [ ] Binary TOON format
- [ ] Compression integration (TOON + gzip)
- [ ] Android Studio plugin for visualization

## 🤝 Contributing

Contributions are welcome! Whether it's:
- 🐛 Bug reports
- 💡 Feature requests
- 📝 Documentation improvements
- 🔧 Code contributions

Please open an issue or PR on GitHub.

## 🌟 Why KToon?

Built by Android engineers who noticed the LLM industry was already benefiting from token-efficient formats. We asked: why should only AI companies get these benefits? Mobile apps face the same challenges—limited bandwidth, expensive data plans, slow networks.

KToon brings proven efficiency techniques to the entire Kotlin ecosystem. Whether you're building a social app, e-commerce platform, or IoT controller, you deserve efficient data serialization.

**The skeleton is ready. What will you build with it?**

---

<div align="center">

Made with ☕ by developers who care about efficiency

[⭐ Star us on GitHub](https://github.com/JosephSanjaya/ktoon) • [📖 Read the docs](ABOUT.md) • [💬 Join discussions](https://github.com/JosephSanjaya/ktoon/discussions)

</div>
