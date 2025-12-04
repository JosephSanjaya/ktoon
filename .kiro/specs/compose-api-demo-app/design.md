
Based 
on the prework analysis, I've identified the following testable properties. Many requirements focus on UI styling and implementation details which aren't suitable for automated testing. The properties below focus on behavioral correctness that can be verified through property-based testing.

### Property Reflection

After reviewing all testable criteria, I've identified several areas where properties can be consolidated:

- **Error handling properties (3.4, 6.2, 6.3)** can be combined into a comprehensive error state property
- **Loading state properties (5.4, 6.1)** represent the same behavior
- **Response size properties (8.1, 8.2)** can be generalized to any format
- **State transition properties (6.4)** are covered by the error handling property

The following properties provide unique validation value without redundancy:

### Property 1: Navigation triggers screen transitions
*For any* clickable navigation element, when clicked, the navigation callback should be invoked and the screen state should change to the target screen.
**Validates: Requirements 1.3**

### Property 2: Pager synchronization with tabs
*For any* tab index in the pager, when the tab is selected, the pager should navigate to the corresponding page index.
**Validates: Requirements 2.4**

### Property 3: Successful API responses are parsed and displayed
*For any* successful API response containing valid user data, the system should parse the response into User objects and update the UI state to display the user list.
**Validates: Requirements 3.3**

### Property 4: Failed API requests show error state with retry
*For any* failed API request, the system should transition to an error state displaying an error message and provide a retry mechanism that clears the error and returns to loading state when invoked.
**Validates: Requirements 3.4, 6.2, 6.3, 6.4**

### Property 5: Response data toggle changes visibility state
*For any* format page, toggling the "Show Response Data" button should change the visibility state of the raw response view and update the button label accordingly.
**Validates: Requirements 4.2, 4.4**

### Property 6: Response metrics are calculated correctly
*For any* API response, the system should calculate the response size in bytes and estimated token count (size / 4) and display these metrics.
**Validates: Requirements 4.5, 8.1, 8.2, 8.4**

### Property 7: User data completeness
*For any* user displayed in the list, all required fields (id, name, email, age) should be present and visible in the UI.
**Validates: Requirements 5.2**

### Property 8: Loading state displays indicator
*For any* API request in progress, the system should display a loading indicator and hide the user list or error state.
**Validates: Requirements 5.4, 6.1**

### Property 9: Token savings calculation
*For any* pair of TOON and JSON responses for the same data, the system should calculate the percentage savings as `((jsonSize - toonSize) / jsonSize) * 100` and display the result.
**Validates: Requirements 8.3**

### Property 10: Theme follows system settings
*For any* system theme setting (light or dark), the app should apply the corresponding Material3 color scheme.
**Validates: Requirements 10.3**

## Error Handling

### Network Errors

**Strategy**: Graceful degradation with user feedback and retry capability.

**Error Types:**
1. **Connection Errors**: Server unreachable, timeout
2. **HTTP Errors**: 4xx, 5xx status codes
3. **Parsing Errors**: Invalid response format

**Handling:**
- Catch all exceptions in API calls using try-catch blocks
- Display user-friendly error messages (avoid technical jargon)
- Provide "Retry" button to attempt the request again
- Clear error state when retry is initiated
- Log errors for debugging (using Napier or similar)

**Example:**
```kotlin
try {
    val (users, rawResponse) = apiClient.getUsersToon()
    // Update success state
} catch (e: Exception) {
    error = when (e) {
        is IOException -> "Network error. Please check your connection."
        is SerializationException -> "Failed to parse response."
        else -> "An unexpected error occurred: ${e.message}"
    }
}
```

### State Management Errors

**Strategy**: Defensive state updates with null safety.

**Handling:**
- Use nullable types for data that may not be loaded yet
- Check for null before rendering dependent UI
- Provide empty states for empty lists
- Ensure state updates are atomic (no partial updates)

### UI Errors

**Strategy**: Compose's error boundaries and safe rendering.

**Handling:**
- Use `remember` with safe default values
- Validate state before rendering
- Provide fallback UI for error states
- Use `LaunchedEffect` with proper cleanup

## Testing Strategy

### Unit Testing

Unit tests will focus on specific examples and edge cases:

**ApiClient Tests:**
- Test successful TOON request returns correct Accept header
- Test successful JSON request returns correct Accept header
- Test error handling for network failures
- Test response parsing for valid data

**State Management Tests:**
- Test navigation state transitions
- Test pager state synchronization with tabs
- Test response data toggle state changes

**Calculation Tests:**
- Test token count calculation (bytes / 4)
- Test savings percentage calculation
- Test edge case: zero-size responses
- Test edge case: identical TOON and JSON sizes

**UI Component Tests:**
- Test UserCard displays all fields correctly
- Test empty state message when user list is empty
- Test loading indicator visibility during API calls

### Property-Based Testing

Property-based tests will verify universal properties across all inputs using a PBT library. For Kotlin Multiplatform, we'll use **Kotest Property Testing** which supports all platforms.

**Configuration:**
- Minimum 100 iterations per property test
- Each test tagged with format: `**Feature: compose-api-demo-app, Property {number}: {property_text}**`
- Tests located in `commonTest` for cross-platform execution

**Property Tests:**

1. **Navigation Property Test**
   - Generate random screen states
   - Verify navigation callbacks change state correctly
   - Tag: `**Feature: compose-api-demo-app, Property 1: Navigation triggers screen transitions**`

2. **Pager Synchronization Test**
   - Generate random tab indices (0, 1)
   - Verify pager navigates to correct page
   - Tag: `**Feature: compose-api-demo-app, Property 2: Pager synchronization with tabs**`

3. **API Response Parsing Test**
   - Generate random valid user data
   - Verify parsing produces correct User objects
   - Tag: `**Feature: compose-api-demo-app, Property 3: Successful API responses are parsed and displayed**`

4. **Error Handling Test**
   - Generate random error types
   - Verify error state is set and retry clears it
   - Tag: `**Feature: compose-api-demo-app, Property 4: Failed API requests show error state with retry**`

5. **Toggle State Test**
   - Generate random initial visibility states
   - Verify toggle changes state correctly
   - Tag: `**Feature: compose-api-demo-app, Property 5: Response data toggle changes visibility state**`

6. **Metrics Calculation Test**
   - Generate random response strings
   - Verify size and token count calculations
   - Tag: `**Feature: compose-api-demo-app, Property 6: Response metrics are calculated correctly**`

7. **Data Completeness Test**
   - Generate random User objects
   - Verify all fields are present in rendered output
   - Tag: `**Feature: compose-api-demo-app, Property 7: User data completeness**`

8. **Loading State Test**
   - Generate random loading states
   - Verify loading indicator visibility
   - Tag: `**Feature: compose-api-demo-app, Property 8: Loading state displays indicator**`

9. **Savings Calculation Test**
   - Generate random TOON and JSON response sizes
   - Verify percentage calculation is correct
   - Tag: `**Feature: compose-api-demo-app, Property 9: Token savings calculation**`

10. **Theme Switching Test**
    - Generate random theme settings (light/dark)
    - Verify correct color scheme is applied
    - Tag: `**Feature: compose-api-demo-app, Property 10: Theme follows system settings**`

### Integration Testing

Integration tests will verify end-to-end flows:

- **Full User Journey**: Home → Ktor Demo → Load TOON data → Toggle response → Load JSON data → Compare
- **Error Recovery**: Trigger network error → Verify error UI → Retry → Verify success
- **Cross-Platform**: Run same tests on Android, iOS, and Desktop

### Manual Testing

Manual testing will cover aspects that cannot be automated:

- Visual design compliance with Material3 guidelines
- Animation smoothness and transitions
- Touch/click responsiveness
- Theme consistency across screens
- Accessibility (screen readers, contrast)

## Platform-Specific Considerations

### Android
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Ktor Engine**: OkHttp
- **Considerations**: 
  - Handle configuration changes (rotation)
  - Test on various screen sizes
  - Verify Material3 dynamic colors work

### iOS
- **Minimum Version**: iOS 14
- **Ktor Engine**: Darwin (NSURLSession)
- **Considerations**:
  - Test on iPhone and iPad
  - Verify safe area handling
  - Test dark mode switching

### Desktop (JVM)
- **JVM Target**: 21
- **Ktor Engine**: CIO
- **Considerations**:
  - Window resizing behavior
  - Keyboard navigation
  - Mouse hover states

### Shared Code
- All UI code in `commonMain`
- Platform-specific code only for entry points
- Use `expect`/`actual` only if absolutely necessary (none expected for this app)

## Dependencies

### Required Dependencies

**Compose Multiplatform:**
- `org.jetbrains.compose.material3:material3` - Material3 components
- `org.jetbrains.compose.foundation:foundation` - Compose foundation
- `org.jetbrains.compose.ui:ui` - Compose UI

**Networking:**
- `io.ktor:ktor-client-core` - Ktor HTTP client
- `io.ktor:ktor-client-content-negotiation` - Content negotiation plugin
- `io.ktor:ktor-serialization-kotlinx-json` - JSON serialization
- `io.ktor:ktor-client-cio` (JVM) - CIO engine for Desktop
- `io.ktor:ktor-client-darwin` (iOS) - Darwin engine for iOS
- `io.ktor:ktor-client-okhttp` (Android) - OkHttp engine for Android

**Project Modules:**
- `ktoon-core` - TOON serialization format
- `ktoon-ktor` - Ktor TOON integration

**Serialization:**
- `org.jetbrains.kotlinx:kotlinx-serialization-core` - Serialization runtime
- `org.jetbrains.kotlinx:kotlinx-serialization-json` - JSON support

**Testing:**
- `io.kotest:kotest-property` - Property-based testing
- `io.kotest:kotest-assertions-core` - Assertions
- `org.jetbrains.kotlin:kotlin-test` - Kotlin test framework

### Dependency Configuration

All dependencies will be added to `composeApp/build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            
            implementation(project(":ktoon-core"))
            implementation(project(":ktoon-ktor"))
            
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
        }
        
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        
        commonTest.dependencies {
            implementation(libs.kotest.property)
            implementation(libs.kotest.assertions.core)
            implementation(kotlin("test"))
        }
    }
}
```

## Implementation Notes

### Simplicity Guidelines

1. **No ViewModels**: Use composable-level state with `remember` and `mutableStateOf`
2. **No Repository Pattern**: Call `ApiClient` directly from composables
3. **No Dependency Injection**: Create `ApiClient` instance directly in `App.kt` and pass down
4. **No Navigation Library**: Use simple sealed class and `when` expression for navigation
5. **Minimal Abstraction**: Keep code straightforward and easy to follow

### State Management Pattern

```kotlin
@Composable
fun FormatPage(format: Format, apiClient: ApiClient) {
    var users by remember { mutableStateOf<List<User>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(format) {
        isLoading = true
        error = null
        try {
            val (loadedUsers, _) = when (format) {
                Format.TOON -> apiClient.getUsersToon()
                Format.JSON -> apiClient.getUsersJson()
            }
            users = loadedUsers
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        } finally {
            isLoading = false
        }
    }
    
    // Render based on state
}
```

### API Client Pattern

```kotlin
class ApiClient(private val baseUrl: String = "http://localhost:8080") {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
            toon()
        }
    }
    
    suspend fun getUsersToon(): Pair<List<User>, String> {
        val response: HttpResponse = client.get("$baseUrl/users") {
            header("Accept", "application/toon")
        }
        val rawText = response.bodyAsText()
        val users = Toon.decodeFromString<List<User>>(rawText)
        return users to rawText
    }
    
    suspend fun getUsersJson(): Pair<List<User>, String> {
        val response: HttpResponse = client.get("$baseUrl/users") {
            header("Accept", "application/json")
        }
        val rawText = response.bodyAsText()
        val users = Json.decodeFromString<List<User>>(rawText)
        return users to rawText
    }
}
```

### Material3 Theme Pattern

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

@Composable
fun DemoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography.Default,
        content = content
    )
}
```

## Future Enhancements

While keeping the current implementation simple, potential future enhancements include:

1. **Retrofit Support**: Implement the Retrofit demo screen with ktoon-retrofit integration
2. **Response Caching**: Cache API responses to avoid redundant requests
3. **Offline Mode**: Store responses locally for offline viewing
4. **More Endpoints**: Add additional API endpoints to demonstrate different data structures
5. **Performance Metrics**: Add detailed performance comparison (request time, parsing time)
6. **Export Functionality**: Allow users to export comparison results
7. **Custom Backend URL**: Allow users to configure the backend URL
8. **WebAssembly Support**: Add web platform support

These enhancements are intentionally excluded from the initial implementation to maintain simplicity and focus on the core demonstration of TOON format benefits.
