# KToon: Rethinking Data Serialization for Mobile

## Inspiration 💡

As Android engineers, we've all been there: watching network traffic in Charles Proxy and seeing the same field names repeated hundreds of times in API responses. A product list with 100 items? That's `"id"`, `"name"`, `"price"` written 100 times. Your users' data plans are paying for that redundancy.

JSON is great for readability, but terrible for efficiency. We started wondering: what if there was a serialization format that actually respected bandwidth? Something that could cut payload sizes by half without sacrificing type safety or developer experience?

We noticed the LLM industry was already benefiting from token-efficient formats like TOON (Token-Oriented Object Notation) treating data like a spreadsheet with headers once and values in rows. Simple concept, massive impact. But why should only AI companies get the benefits? Mobile apps deal with the same problems: limited bandwidth, expensive data plans, slow networks.

That's when we decided to build a Kotlin serialization library that brings TOON's efficiency to the entire mobile ecosystem—not just LLM use cases. Whether you're building a social app, an e-commerce platform, or an IoT controller, you deserve efficient data serialization.

## What it does 🎯

KToon is a Kotlin Multiplatform serialization library that cuts network payload sizes by 30-60% while keeping your code clean and type-safe.

Instead of this JSON blob:
```json
[
  {"id": 1, "name": "Alice", "age": 30},
  {"id": 2, "name": "Bob", "age": 25}
]
```

You get this compact TOON format:
```
users[2]{id,name,age}:
  1,Alice,30
  2,Bob,25
```

Same data. Half the bytes. Faster loading. Happier users.

**Why Android devs should care:**
- **Smaller APK assets**: Store config files and seed data more efficiently
- **Faster API responses**: Less data = faster parsing = smoother UX
- **Offline-first apps**: Sync more data with less storage
- **IoT & wearables**: Perfect for bandwidth-constrained devices
- **Drop-in replacement**: Works with your existing `@Serializable` classes
- **True multiplatform**: Android, iOS, Desktop, Web (JS + WASM)

**Skeleton Crew Challenge:**
We built a lean, flexible foundation that powers two completely different use cases:
1. **API Demo App**: Network-focused app showing real-time format comparison with live backend
2. **Offline Data Manager**: Local-first app demonstrating efficient data storage and sync

Both built from the same ktoon-core skeleton, proving the format's versatility.

## How we built it 🛠️

**The Skeleton: ktoon-core**

We built a minimal but powerful serialization engine using kotlinx.serialization's `AbstractEncoder` and `AbstractDecoder`. The core is just ~1000 lines of pure Kotlin—no platform-specific code, no dependencies beyond stdlib.

The secret sauce is a state machine with two modes:
1. **Indentation mode** for nested objects (like YAML but simpler)
2. **Table mode** for collections (CSV-style with headers)

**Architecture:**
```
ktoon-core (the skeleton)
    ├── Toon.kt - Public API (StringFormat)
    ├── ToonEncoder.kt - Serialization with state machine
    ├── ToonDecoder.kt - Deserialization with error handling
    └── ToonLexer.kt - Tokenization and parsing
```

**Application 1: API Demo App (Network-focused)**
Built with Compose Multiplatform to showcase real-time format comparison:
- Live backend server with dual JSON/TOON endpoints
- Side-by-side format comparison
- Real-time byte count and savings metrics
- Interactive API testing with Ktor client integration
- Demonstrates: Network efficiency, API integration, content negotiation

**Application 2: Offline Data Manager (Storage-focused)**
Demonstrates local-first architecture:
- Efficient local data storage using TOON format
- Batch sync operations with minimal bandwidth
- Configuration file management
- Seed data bundling in APK
- Demonstrates: Storage efficiency, offline-first patterns, data portability

Both apps share the same ktoon-core foundation but solve completely different problems—proving the skeleton's versatility.

**Tech stack:**
- Kotlin 2.2.21 with Compose Multiplatform
- kotlinx.serialization for type-safe serialization
- Ktor for HTTP (client + server)
- Custom lexer with indentation tracking
- Single-pass O(N) serialization (no buffering)

## Challenges we ran into 😅

**Building a truly minimal skeleton:**
Our first version was 3000+ lines with tons of abstractions. We kept asking "do we really need this?" and cutting until we hit the essence. Getting to ~1000 lines of pure, dependency-free Kotlin took discipline.

**The state machine dance:**
Switching between indentation mode (for objects) and table mode (for collections) mid-serialization was tricky. We had to track encoding state carefully to know when to write headers vs values vs nested structures.

**Null handling in CSV rows:**
How do you represent null in a CSV row without breaking parsing? Empty value? Special keyword? We ended up with both: empty values in table mode, `null` keyword in indentation mode. Took way too many test failures to get right.

**Cross-platform determinism:**
Getting identical output on Android, iOS, JVM, JS, and WASM was harder than expected. Different platforms have different string builders and number formatting. We had to be very explicit about everything.

**Making it actually reusable:**
The Skeleton Crew challenge forced us to think: "Can someone else build something completely different with this?" We refactored multiple times to remove assumptions and keep the API surface minimal but powerful.

## Accomplishments that we're proud of 🏆

**The skeleton itself:**
- **~1000 lines of pure Kotlin** - No platform-specific code, no heavy dependencies
- **Zero expect/actual declarations** - Works everywhere out of the box
- **Single-pass O(N) serialization** - Fast enough for production
- **Comprehensive error messages** - Line numbers, context snippets, helpful suggestions

**Proven versatility:**
- **Two distinct applications** from the same foundation
- **67.4% size reduction** verified with real data (553 bytes JSON → 179 bytes TOON)
- **Full kotlinx.serialization support** - @SerialName, @Transient, @Polymorphic, custom serializers
- **Working Ktor integration** for both client and server

**Developer experience:**
- Drop-in replacement for JSON - your existing `@Serializable` classes just work
- Clear API surface - `Toon.encodeToString()` and `Toon.decodeFromString()`
- Actual runnable demos you can clone and experiment with

## What we learned 📚

**Less is more:**
Building a skeleton forced us to identify what's truly essential. Every feature had to justify its existence. The result is cleaner, more maintainable, and easier to extend.

**kotlinx.serialization is underrated:**
The AbstractEncoder/AbstractDecoder API is incredibly powerful. You can build custom formats that integrate seamlessly with existing Kotlin code. More Android devs should explore this.

**State machines > ad-hoc logic:**
Our encoder switches between modes seamlessly because we modeled it as a proper state machine from day one. No spaghetti code, no weird edge cases.

**Format design matters:**
TOON's table mode (headers + rows) is intuitive because it mirrors how humans think about tabular data. Good format design makes implementation easier.

**Versatility comes from constraints:**
By keeping ktoon-core minimal and focused, we accidentally made it more reusable. The two demo apps prove you can build very different things from the same foundation.

**Multiplatform is the future:**
Writing once and running on Android, iOS, Desktop, and Web (including WASM!) is still magical. Kotlin Multiplatform is ready for production.

## What's next for KToon 🚀

**Improve the skeleton:**
- Property-based testing for bulletproof correctness
- Streaming support for huge datasets (>10MB)
- Schema validation and versioning
- Even leaner core (can we get under 800 lines?)

**More integrations:**
- Retrofit support (not just Ktor)
- Room database integration for efficient local storage
- DataStore integration for preferences
- OkHttp interceptor for transparent format switching

**New applications:**
- **Config Manager**: Type-safe app configuration with TOON files
- **Analytics Batcher**: Efficient event batching for analytics SDKs
- **Sync Engine**: Minimal bandwidth data synchronization
- **Debug Tool**: Developer tool for inspecting serialized data

**Community:**
- Official Maven Central release
- Comprehensive documentation site
- Video tutorials for Android devs
- Real-world case studies from production apps

**Dream features:**
- Binary TOON format for even more savings
- Compression integration (TOON + gzip)
- Code generation for even faster serialization
- Android Studio plugin for format visualization

---

Built by Android engineers who care about efficiency, for Android engineers who want to build something unique.

**The skeleton is ready. What will you build with it?**
