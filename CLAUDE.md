# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a full-stack starter project combining:
- **Backend**: Spring Boot 3.5.5 with Java 21
- **Frontend**: Angular 20 with Material Design
- **Build System**: Maven with Frontend Maven Plugin integration

The project uses a monorepo structure where the Angular frontend is located in `src/main/frontend/` and is built as part of the Maven lifecycle.

## Architecture

### Backend (Spring Boot)
- Main application: `src/main/java/org/tanzu/materialstarter/MaterialStarterApplication.java`
- Package structure: `org.tanzu.materialstarter`
- Uses Spring Boot Starter Web and Actuator
- Application properties: `src/main/resources/application.properties`
- Tests: `src/test/java/org/tanzu/materialstarter/`

### Frontend (Angular)
- Location: `src/main/frontend/`
- Angular 20 with standalone components (no NgModules)
- Material Design 3 theming with Azure primary palette
- Uses SCSS for styling
- TypeScript configuration with strict mode
- Karma + Jasmine for testing

## Development Commands

### Full Stack Development
```bash
# Build entire project (backend + frontend)
./mvnw clean package

# Run Spring Boot application (includes built frontend)
./mvnw spring-boot:run

# Clean build
./mvnw clean
```

### Frontend Only (in src/main/frontend/)
```bash
# Install dependencies
npm ci

# Development server (http://localhost:4200)
npm start
# or
ng serve

# Build frontend
npm run build
# or
ng build

# Run tests
npm test
# or
ng test

# Watch mode for development
npm run watch
```

### Testing
```bash
# Run Spring Boot tests
./mvnw test

# Run Angular tests (in src/main/frontend/)
ng test
```

## Key Technologies & Patterns

### Frontend
- **Angular 20**: Uses modern standalone components, signals, and new control flow
- **Material Design**: Pre-configured with mat.theme() and system variables
- **Styling**: SCSS with Material 3 design tokens
- **Component Structure**: Uses separate files (.ts, .html, .scss)
- **Prettier**: Configured with 100 character line width and single quotes

### Backend
- **Java 21**: Modern Java features preferred (records, lambdas)
- **Spring Boot**: RESTful web services with embedded server
- **Maven**: Frontend Maven Plugin handles Node.js/npm integration

## Build Integration

The Maven build automatically:
1. Installs Node.js v22.12.0 and npm in `target/`
2. Runs `npm ci` to install frontend dependencies
3. Executes `ng build` to create production frontend build
4. Packages everything into a single Spring Boot JAR

Frontend build output goes to `dist/` and gets served by Spring Boot as static resources.

## Code Style Preferences

- **Java**: Use records and lambdas where appropriate
- **Angular**: Use signals and new template control flow syntax
- **Material UI**: Follow Material Design 3 standards and system variables
- **Formatting**: Prettier configured for consistent code style