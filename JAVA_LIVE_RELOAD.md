# Java Live Reload Setup for LDAP Browser

## What's Been Fixed

1. **Added Spring Boot DevTools** - Enables automatic restart when Java classes change
2. **Configured Development Properties** - Optimized settings for live reload
3. **Enhanced Maven Configuration** - Better compilation support for development
4. **Created Development Script** - Easy way to start with live reload enabled

## How to Use Live Reload

### Method 1: Using the Development Script (Recommended)
```bash
./dev-reload.sh
```

### Method 2: Using Maven directly
```bash
mvn spring-boot:run -Dspring.profiles.active=development
```

### Method 3: Using VS Code Task
- Press `Ctrl+Shift+P` and search for "Tasks: Run Task"
- Select "Run LDAP Browser"

## How Live Reload Works

1. **Java Classes**: When you modify any Java class and save it, Spring Boot DevTools will automatically restart the application
2. **Properties Files**: Changes to application properties are picked up automatically
3. **Frontend Assets**: Vaadin handles frontend live reload separately

## Testing Live Reload

1. Start the application using one of the methods above
2. Open a Java class (e.g., `MainView.java`)
3. Make a small change (add a comment or modify text)
4. Save the file
5. Watch the console - you should see the application restart automatically
6. Refresh your browser to see the changes

## Development Tips

- **Keep the console open** to see restart notifications
- **Use incremental compilation** in your IDE for faster restarts
- **Exclude static resources** from restart triggers (already configured)
- **Use development profile** for optimal settings

## Browser LiveReload

For complete live reload including browser refresh, install the LiveReload browser extension:
- Chrome: LiveReload Extension
- Firefox: LiveReload Add-on

The LiveReload server runs on port 35729 (configured in development properties).

## Troubleshooting

If live reload isn't working:

1. Check that DevTools is in the classpath (should see "LiveReload server is running" in logs)
2. Verify you're not running in production mode
3. Make sure you're modifying files in `src/main/java` (not `target/`)
4. Check IDE compilation settings - some IDEs need "Build automatically" enabled
