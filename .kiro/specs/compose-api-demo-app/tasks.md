# Implementation Plan

- [x] 1. Set up project dependencies and configuration




  - Add Ktor client dependencies (core, content-negotiation, platform engines)
  - Add ktoon-core and ktoon-ktor project dependencies
  - Add Compose Material3 dependencies
  - Add kotlinx-serialization dependencies
  - Configure serialization plugin in build.gradle.kts
  - _Requirements: 3.5, 7.5, 9.2_

- [x] 2. Create data models and API client




- [x] 2.1 Create User data model


  - Define User data class with @Serializable annotation
  - Include fields: id (Int), name (String), email (String), age (Int)
  - _Requirements: 5.2_

- [x] 2.2 Implement ApiClient class


  - Create ApiClient with HttpClient configured for JSON and TOON
  - Implement getUsersToon() method with Accept: application/toon header
  - Implement getUsersJson() method with Accept: application/json header
  - Return both parsed users and raw response text
  - Add proper error handling with try-catch
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ]* 2.3 Write property test for API response parsing
  - **Property 3: Successful API responses are parsed and displayed**
  - **Validates: Requirements 3.3**

- [ ]* 2.4 Write property test for error handling
  - **Property 4: Failed API requests show error state with retry**
  - **Validates: Requirements 3.4, 6.2, 6.3, 6.4**

- [x] 3. Create Material3 theme




- [x] 3.1 Implement DemoAppTheme composable


  - Define light and dark color schemes
  - Create theme composable with system theme detection
  - Use Material3 colorScheme and typography
  - _Requirements: 10.1, 10.2, 10.3_

- [ ]* 3.2 Write property test for theme switching
  - **Property 10: Theme follows system settings**
  - **Validates: Requirements 10.3**

- [x] 4. Implement navigation and main app structure



- [x] 4.1 Create Screen sealed class for navigation


  - Define Screen.Home and Screen.KtorDemo objects
  - _Requirements: 9.5_

- [x] 4.2 Implement App.kt main composable


  - Create navigation state with remember and mutableStateOf
  - Wrap content in DemoAppTheme
  - Implement when expression for screen routing
  - Create ApiClient instance and pass to screens
  - _Requirements: 1.1, 9.3, 9.5_

- [ ]* 4.3 Write property test for navigation
  - **Property 1: Navigation triggers screen transitions**
  - **Validates: Requirements 1.3**

- [x] 5. Implement HomeScreen



- [x] 5.1 Create HomeScreen composable

  - Use Scaffold with centered content
  - Create two ElevatedCard components for Ktor and Retrofit
  - Add onClick handler for Ktor card to navigate
  - Add onClick handler for Retrofit card to show snackbar
  - Use Material3 components with proper styling
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ]* 5.2 Write unit tests for HomeScreen
  - Test Ktor card click triggers navigation callback
  - Test Retrofit card shows "Coming Soon" snackbar
  - _Requirements: 1.3, 1.4_

- [x] 6. Implement KtorDemoScreen with pager



- [x] 6.1 Create KtorDemoScreen composable

  - Use Scaffold with TopAppBar and back button
  - Create PagerState with rememberPagerState for 2 pages
  - Implement TabRow with "TOON Format" and "JSON Format" tabs
  - Implement HorizontalPager with two FormatPage instances
  - Sync tab selection with pager state
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 6.2 Write property test for pager synchronization
  - **Property 2: Pager synchronization with tabs**
  - **Validates: Requirements 2.4**

- [x] 7. Implement FormatPage



- [x] 7.1 Create FormatPage composable with state management


  - Define state variables: users, rawResponse, isLoading, error, showRawResponse, responseSize
  - Use LaunchedEffect to load data when format changes
  - Implement API call with error handling
  - Calculate response size and token count
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 6.1, 6.2, 6.4, 8.1, 8.2, 8.4_

- [x] 7.2 Implement FormatPage UI layout


  - Create statistics card showing size, tokens, and savings
  - Add "Show/Hide Response Data" button with toggle logic
  - Conditionally render ResponseDataView when visible
  - Render loading indicator when isLoading is true
  - Render error message with retry button when error exists
  - Render UserListView when users are loaded
  - _Requirements: 4.1, 4.2, 4.4, 5.4, 6.1, 6.2, 6.3, 8.5_

- [ ]* 7.3 Write property test for response data toggle
  - **Property 5: Response data toggle changes visibility state**
  - **Validates: Requirements 4.2, 4.4**

- [ ]* 7.4 Write property test for response metrics calculation
  - **Property 6: Response metrics are calculated correctly**
  - **Validates: Requirements 4.5, 8.1, 8.2, 8.4**

- [ ]* 7.5 Write property test for loading state
  - **Property 8: Loading state displays indicator**
  - **Validates: Requirements 5.4, 6.1**

- [x] 8. Implement ResponseDataView component



- [x] 8.1 Create ResponseDataView composable

  - Use Card with proper padding
  - Display format label (TOON or JSON)
  - Show response size in bytes
  - Show estimated token count
  - Display raw response in scrollable Text with monospace font
  - _Requirements: 4.2, 4.3, 4.5_

- [x] 9. Implement UserListView component





- [x] 9.1 Create UserListView composable

  - Use LazyColumn for user list
  - Create UserCard composable for each user
  - Display all user fields: id, name, email, age
  - Use ElevatedCard with proper spacing and elevation
  - Handle empty list with empty state message
  - _Requirements: 5.1, 5.2, 5.3, 5.5_

- [ ]* 9.2 Write property test for user data completeness
  - **Property 7: User data completeness**
  - **Validates: Requirements 5.2**

- [x] 10. Implement token savings calculation



- [x] 10.1 Add savings calculation logic to FormatPage

  - Calculate percentage savings when both TOON and JSON are loaded
  - Use formula: ((jsonSize - toonSize) / jsonSize) * 100
  - Display savings in statistics card
  - _Requirements: 8.3_

- [ ]* 10.2 Write property test for savings calculation
  - **Property 9: Token savings calculation**
  - **Validates: Requirements 8.3**

- [ ] 11. Configure platform-specific entry points
- [ ] 11.1 Update androidMain MainActivity
  - Call App() composable from setContent
  - Ensure proper theme wrapping
  - _Requirements: 7.1, 7.4_

- [ ] 11.2 Update iosMain MainViewController
  - Call App() composable from ComposeUIViewController
  - Ensure proper theme wrapping
  - _Requirements: 7.2, 7.4_

- [ ] 11.3 Update jvmMain main function
  - Call App() composable from application window
  - Ensure proper theme wrapping
  - _Requirements: 7.3, 7.4_

- [ ] 12. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 13. Final polish and verification
- [ ] 13.1 Verify Material3 styling consistency
  - Check all components use Material3 design tokens
  - Verify proper spacing and padding throughout
  - Test light and dark themes on all platforms
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 13.2 Test end-to-end user flows
  - Test: Home → Ktor → Load TOON → Show response → Load JSON → Compare
  - Test: Error scenario → Retry → Success
  - Test: Retrofit "Coming Soon" message
  - Test: Pager swiping and tab navigation
  - _Requirements: All_

- [ ] 14. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
