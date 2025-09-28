# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**IPZS Legislative Chat Assistant** - A full-stack AI-powered chat application for legislation document analysis and querying, combining:
- **Backend**: Spring Boot 3.5.5 with Java 21, Spring AI integration
- **Frontend**: Angular 20 with Material Design
- **AI/ML**: OpenAI GPT-4 Turbo for chat, vector database with pgvector for document embeddings
- **Database**: PostgreSQL with vector store for semantic search
- **Build System**: Maven with Frontend Maven Plugin integration

The project uses a monorepo structure where the Angular frontend is located in `src/main/frontend/` and is built as part of the Maven lifecycle.

## Architecture

### Backend (Spring Boot)
- Main application: `src/main/java/org/tanzu/ipzs/LegislationAssistantApplication.java`
- Package structure: `org.tanzu.ipzs.legislation`
- Core components:
  - **Controllers**: REST APIs for chat (`ChatController`), documents (`DocumentController`), sample data (`SampleDataController`)
  - **Models**: DTOs and entities for legislation documents and chat messages
  - **Services**: Document processing with Apache Tika and PDFBox, vector store operations
  - **Configuration**: Spring AI setup with OpenAI integration, pgvector configuration, date-aware advisors
- Database: PostgreSQL with vector store extension for semantic document search
- Document processing: Apache Tika and PDFBox for parsing legislation documents
- Application properties: `src/main/resources/application.properties`

### Frontend (Angular)
- Location: `src/main/frontend/`
- Angular 20 with standalone components (no NgModules)
- Feature-based architecture:
  - **Chat features**: Chat container, input components, date selector
  - **Core services**: ChatService for AI interactions, DocumentService for file operations
  - **Models**: TypeScript interfaces for documents and chat messages
- Material Design 3 theming with internationalization support
- Translation service for multi-language support
- Uses SCSS for styling
- TypeScript configuration with strict mode
- Karma + Jasmine for testing

## Development Commands

### Prerequisites
- PostgreSQL database running on localhost:5432 with database `ipzs_legislation`
- OpenAI API key set in environment variable `OPENAI_API_KEY`

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

### Database Setup
```bash
# Create PostgreSQL database
createdb ipzs_legislation

# Database will be initialized automatically on first run with pgvector extension
```

## Key Technologies & Patterns

### Frontend
- **Angular 20**: Uses modern standalone components, signals, and new control flow
- **Material Design**: Pre-configured with mat.theme() and system variables
- **Internationalization**: TranslationService for English/German language switching
- **Styling**: SCSS with Material 3 design tokens
- **Component Structure**: Feature-based organization with separate files (.ts, .html, .scss)
- **Services**: Reactive patterns with RxJS for chat and document operations
- **Prettier**: Configured with 100 character line width and single quotes

### Backend
- **Java 21**: Modern Java features preferred (records, lambdas)
- **Spring Boot**: RESTful web services with embedded server
- **Spring AI**: Integration with OpenAI GPT-4 Turbo for chat completions
- **Vector Database**: pgvector extension for PostgreSQL to store document embeddings
- **Document Processing**: Apache Tika and PDFBox for parsing legislation documents
- **JPA/Hibernate**: Entity management with PostgreSQL dialect
- **Advisors**: Custom date-aware question answering with vector similarity search
- **Maven**: Frontend Maven Plugin handles Node.js/npm integration

### AI/ML Components
- **Embeddings**: OpenAI text-embedding-ada-002 model for document vectorization
- **Chat Model**: GPT-4 Turbo for conversational AI responses
- **Vector Store**: pgvector with 1536 dimensions, 0.7 similarity threshold
- **Date Awareness**: Custom advisor for handling temporal queries about legislation

## Build Integration

The Maven build automatically:
1. Installs Node.js v24.9.0 and npm in `target/`
2. Runs `npm ci` to install frontend dependencies
3. Executes `ng build` to create production frontend build
4. Packages everything into a single Spring Boot JAR

Frontend build output goes to `dist/` and gets served by Spring Boot as static resources.

## Code Style Preferences

- **Java**: Use records and lambdas where appropriate
- **Angular**: Use signals and new template control flow syntax
- **Material UI**: Follow Material Design 3 standards and system variables
- **Formatting**: Prettier configured for consistent code style

## Environment Configuration

Required environment variables:
- `OPENAI_API_KEY`: Your OpenAI API key for GPT-4 and embeddings
- Database connection details are configured in `application.properties`

The application supports Cloud Foundry environments via java-cfenv-boot-tanzu-genai dependency.