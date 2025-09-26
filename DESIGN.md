# IPZS Legislation Assistant - Design & Current Implementation Status

## Project Overview

This document outlines the current implementation status of the legislation knowledge management system for **Istituto Poligrafico e Zecca dello Stato** (IPZS). The system leverages Retrieval Augmented Generation (RAG) patterns with Spring AI 1.1.0-M2 to enable intelligent querying of Italian legislation through a conversational interface.

**Current Status**: Initial development phase with basic chat functionality implemented.

## Executive Summary

The system is designed to provide:
- ✅ **Basic Chat Interface**: Angular 20 frontend with Material Design
- ✅ **Spring AI Integration**: RAG-enabled chat client with vector store support
- ✅ **Modern Architecture**: Java 21 with Spring Boot 3.5.5
- 🚧 **Vector Store Setup**: PostgreSQL + PgVector configuration ready
- 📋 **Pending**: Document ingestion and processing pipeline
- 📋 **Pending**: Temporal context awareness for historically accurate queries
- 📋 **Pending**: Cloud-native deployment on Cloud Foundry

**Legend**: ✅ Implemented | 🚧 In Progress | 📋 Planned

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Cloud Foundry Platform                   │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────┐        ┌─────────────────────────┐     │
│  │   Angular 20    │        │    Spring Boot 3.5      │     │
│  │   Frontend      │◄──────►│    REST API Layer       │     │
│  │   (Material UI) │        └────────┬─────────────────┘     │
│  └─────────────────┘                 │                       │
│                                       ▼                       │
│                          ┌────────────────────────┐          │
│                          │   Spring AI 1.1.0-M2   │          │
│                          │   RAG Implementation   │          │
│                          └────────┬───────────────┘          │
│                                   │                          │
│                                   ▼                          │
│                      ┌─────────────────────────────┐        │
│                      │   PostgreSQL + PgVector     │        │
│                      │   Vector Database           │        │
│                      └─────────────────────────────┘        │
└───────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

1. User interacts with Angular chatbot interface
2. Frontend sends queries to Spring Boot REST API
3. Spring AI processes queries using RAG pattern
4. Vector similarity search in PgVector with metadata filtering
5. LLM generates contextual responses
6. Response returned to user interface

## Technology Stack

### Backend Technologies
- **Java 21** - Modern Java features with records and lambdas
- **Spring Boot 3.5.5** - Application framework
- **Spring AI 1.1.0-M2** - AI/ML integration framework
- **PostgreSQL 15+** - Primary database
- **PgVector Extension** - Vector similarity search
- **OpenAI/Azure OpenAI** - LLM provider (configurable)
- **Apache PDFBox** - PDF text extraction
- **Apache Tika** - Document parsing

### Frontend Technologies
- **Angular 20** - Modern frontend framework with signals and zoneless change detection
- **Angular Material 20** - Material Design 3 components
- **TypeScript 5.9** - Type-safe JavaScript
- **SCSS** - Styling with Material Design tokens
- **RxJS 7** - Reactive programming
- **Standalone Components** - No NgModules, modern Angular architecture

### Infrastructure & Deployment
- **Cloud Foundry** - PaaS platform (configured)
- **Maven 3.9+** - Build automation with Frontend Maven Plugin
- **Node.js v24.9.0** - Frontend build environment
- **Docker** - Containerization (for local development)
- **GitHub Actions** - CI/CD pipeline (planned)

## Database Design

### PostgreSQL Schema

```sql
-- Enable PgVector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Legislation documents table
CREATE TABLE legislation_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id VARCHAR(255) UNIQUE NOT NULL,
    title TEXT NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    publication_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    expiration_date DATE,
    issuing_authority VARCHAR(255),
    document_number VARCHAR(100),
    original_url TEXT,
    raw_content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Document chunks for vector storage
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID REFERENCES legislation_documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536), -- OpenAI ada-002 dimension
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

-- Indices for performance
CREATE INDEX idx_doc_chunks_embedding ON document_chunks 
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX idx_doc_chunks_metadata ON document_chunks 
    USING GIN (metadata);

CREATE INDEX idx_legislation_dates ON legislation_documents 
    (effective_date, expiration_date);

-- Chat sessions
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_identifier VARCHAR(255),
    date_context DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_interaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat messages
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES chat_sessions(id) ON DELETE CASCADE,
    message_type VARCHAR(20) NOT NULL, -- 'USER' or 'ASSISTANT'
    content TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Backend Implementation

### Package Structure (Current Implementation)

```
org.tanzu.ipzs.legislation
├── config/
│   ├── SpringAIConfig.java              ✅ Implemented
│   └── DatabaseInitializer.java         ✅ Implemented
├── controller/
│   ├── ChatController.java              ✅ Implemented
│   └── DocumentController.java          ✅ Implemented
├── model/
│   ├── dto/
│   │   ├── ChatRequestDto.java (record) ✅ Implemented
│   │   ├── ChatResponseDto.java (record)✅ Implemented
│   │   └── DocumentMetadataDto.java     ✅ Implemented
│   └── entity/
│       ├── LegislationDocument.java     ✅ Implemented
│       ├── DocumentChunk.java           ✅ Implemented
│       └── ChatSession.java             ✅ Implemented
├── service/                             📋 Planned
│   ├── ChatService.java
│   ├── DocumentIngestionService.java
│   ├── VectorSearchService.java
│   └── RAGService.java
├── repository/                          📋 Planned
│   ├── LegislationDocumentRepository.java
│   ├── DocumentChunkRepository.java
│   └── ChatSessionRepository.java
├── vectorstore/                         📋 Planned
│   ├── PgVectorStore.java
│   └── MetadataFilter.java
└── util/                                📋 Planned
    ├── DocumentParser.java
    └── ChunkingStrategy.java
```

### Key Spring AI Components

#### 1. Current Spring AI Configuration

```java
@Configuration
public class SpringAIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        return chatClientBuilder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }
}
```

**Current Implementation**: Basic ChatClient with QuestionAnswerAdvisor configured. The vector store is auto-configured via Spring AI starter.

#### 2. Chat Controller (Implemented)

```java
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {

    private final ChatClient chatClient;

    @PostMapping
    public ChatResponseDto chat(@RequestBody ChatRequestDto request) {
        String response = chatClient
                .prompt(request.message())
                .call()
                .content();

        return new ChatResponseDto(
                response,
                request.sessionId() != null ? request.sessionId() : UUID.randomUUID(),
                List.of()
        );
    }
}
```

#### 3. Application Configuration

```properties
# Application
spring.application.name=ipzs-legislation-assistant

# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/ipzs_legislation
spring.datasource.username=postgres
spring.datasource.password=postgres

# Spring AI Configuration
spring.ai.openai.api-key=${OPENAI_API_KEY:your-api-key-here}
spring.ai.openai.embedding.model=text-embedding-ada-002
spring.ai.openai.chat.model=gpt-4-turbo

# Vector Store Configuration
spring.ai.vectorstore.pgvector.dimension=1536
spring.ai.vectorstore.pgvector.similarity-threshold=0.7
```

**Note**: Date filtering and advanced RAG features are planned for future implementation.

### Document Ingestion Pipeline (Planned)

📋 **Future Implementation**:
1. **Document Upload**: Accept PDF/DOCX/HTML formats
2. **Text Extraction**: Use Apache Tika for content extraction
3. **Chunking**: Split documents into semantic chunks (500-1000 tokens)
4. **Metadata Extraction**: Parse dates, authorities, document types
5. **Embedding Generation**: Create vector embeddings via OpenAI
6. **Storage**: Save chunks with embeddings to PgVector

**Dependencies Ready**: Apache Tika and PDFBox dependencies are configured in pom.xml.

## Frontend Implementation

### Angular Application Structure (Current Implementation)

```
src/main/frontend/src/app/
├── core/                                ✅ Implemented
│   ├── services/
│   │   ├── chat.service.ts              ✅ Implemented
│   │   └── document.service.ts          ✅ Implemented
│   └── models/
│       ├── chat-message.model.ts        ✅ Implemented
│       └── document.model.ts            ✅ Implemented
├── features/                            ✅ Partially Implemented
│   └── chat/
│       ├── chat-container/              ✅ Implemented
│       ├── chat-input/                  ✅ Implemented
│       ├── chat-messages/               ✅ Implemented
│       └── date-selector/               ✅ Implemented
├── shared/                              ✅ Implemented
│   └── material/
│       └── material.module.ts           ✅ Implemented
├── app.ts                               ✅ Implemented
├── app.config.ts                        ✅ Implemented (with zoneless)
└── app.routes.ts                        ✅ Implemented

📋 **Planned**:
└── admin/
    ├── document-upload/
    └── document-list/
```

### Key Frontend Components

#### 1. Modern Angular Features (Implemented)

**App Configuration with Zoneless Change Detection**:
```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(),
    provideAnimationsAsync()
  ]
};
```

**Standalone App Component**:
```typescript
@Component({
  selector: 'app-root',
  imports: [ChatContainerComponent],
  template: '<app-chat-container></app-chat-container>',
  styleUrl: './app.scss'
})
export class App {}
```

#### 2. Chat Container with Modern Angular Features

**Key Features Implemented**:
- ✅ **Signals**: Used for reactive state management
- ✅ **Standalone Components**: No NgModules required
- ✅ **Template Control Flow**: Modern `@if` syntax for loading states
- ✅ **Material Design 3**: Full Material UI integration
- ✅ **Zoneless Change Detection**: Better performance

```typescript
@Component({
  selector: 'app-chat-container',
  standalone: true,
  imports: [CommonModule, MaterialModule, ChatInputComponent, ChatMessagesComponent, DateSelectorComponent],
  template: `
    <div class="chat-container">
      <mat-card class="chat-card">
        <mat-card-header>
          <div class="header-content">
            <div class="title-section">
              <mat-card-title>Assistente Legislativo IPZS</mat-card-title>
              <mat-card-subtitle>Sistema di consultazione legislativa intelligente</mat-card-subtitle>
            </div>
            <app-date-selector [currentDate]="chatService.dateContext()" (dateChange)="onDateChange($event)">
            </app-date-selector>
          </div>
        </mat-card-header>

        @if (isLoading()) {
          <div class="loading-overlay">
            <mat-spinner diameter="40"></mat-spinner>
          </div>
        }
      </mat-card>
    </div>
  `
})
export class ChatContainerComponent {
  isLoading = signal(false);
  constructor(public chatService: ChatService) {}
}
```

## Cloud Foundry Deployment (Configured)

The project includes Cloud Foundry configuration through:
- ✅ **CF Environment Dependencies**: `java-cfenv-boot` and `java-cfenv-boot-tanzu-genai`
- ✅ **Application Name**: `ipzs-legislation-assistant`
- 🚧 **Manifest File**: Ready for CF deployment

### Maven Build Integration

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.1</version>
    <configuration>
        <workingDirectory>src/main/frontend</workingDirectory>
        <installDirectory>target</installDirectory>
    </configuration>
    <executions>
        <!-- Install Node.js v24.9.0 and npm -->
        <!-- npm ci -->
        <!-- ng build -->
    </executions>
</plugin>
```

**Build Process**:
1. Maven installs Node.js v24.9.0 and npm
2. Runs `npm ci` to install dependencies
3. Executes `ng build` for production frontend
4. Packages everything into Spring Boot JAR

## Implementation Status & Next Steps

### ✅ Completed (Phase 1)
- ✅ Project structure with Spring AI 1.1.0-M2
- ✅ Basic Spring Boot REST endpoints
- ✅ Angular 20 project with Material Design
- ✅ Modern Angular features (signals, zoneless, standalone components)
- ✅ Basic chat interface functionality
- ✅ Maven build integration with Frontend Maven Plugin

### 🚧 In Progress (Phase 2)
- 🚧 Database schema implementation (entities created)
- 🚧 Vector Store configuration (dependencies ready)

### 📋 Next Steps (Phases 2-3)
- **Immediate Priority**:
  1. Implement JPA repositories
  2. Set up PgVector database initialization
  3. Create document ingestion REST endpoints
  4. Implement file upload functionality

- **Short Term**:
  1. Add Apache Tika text extraction
  2. Implement document chunking strategy
  3. Connect vector store to chat functionality
  4. Add metadata filtering for date context

### 📋 Future Phases (4-7)
- **RAG Implementation**: Advanced prompt engineering and conversation context
- **Frontend Enhancements**: Admin interface for document management
- **Testing & Optimization**: Performance and security improvements
- **Deployment**: Cloud Foundry production configuration

## Technical Architecture Decisions

### Current Technology Choices
- **Java 21**: Modern language features (records, pattern matching)
- **Spring Boot 3.5.5**: Latest stable version with native support
- **Angular 20**: Cutting-edge frontend with signals and zoneless change detection
- **Material Design 3**: Modern UI/UX standards
- **Spring AI 1.1.0-M2**: Latest RAG and vector store capabilities

### Development Commands
```bash
# Backend Development
./mvnw spring-boot:run

# Frontend Development (in src/main/frontend/)
npm start  # Development server on :4200

# Full Build
./mvnw clean package

# Frontend Tests
cd src/main/frontend && npm test
```

## Success Metrics (Future Implementation)

1. **Chat Functionality**: ✅ Basic chat working
2. **UI/UX Quality**: ✅ Material Design implementation
3. **Build Process**: ✅ Integrated Maven + npm build
4. **Code Quality**: ✅ Modern language constructs (Java 21, Angular 20)
5. **Architecture**: ✅ Monorepo structure with clear separation

## Current Status Summary

### ✅ What's Working
- **Full-stack development environment** with integrated build process
- **Modern Angular 20 chat interface** with Material Design
- **Basic Spring AI integration** with ChatClient and vector store support
- **Proper project structure** following best practices
- **All dependencies configured** for document processing and vector storage

### 🚧 In Development
- Database initialization and JPA repositories
- Vector store integration with actual document data
- Document upload and processing pipeline

### 📋 Ready for Implementation
- Apache Tika and PDFBox for document processing
- PostgreSQL + PgVector for vector storage
- Cloud Foundry deployment configuration
- Italian legislation-specific prompts and workflows

## Next Development Priorities

1. **Complete Database Setup**: Implement repositories and database initialization
2. **Document Processing**: Create upload endpoints and text extraction services
3. **Vector Integration**: Connect processed documents to chat functionality
4. **Admin Interface**: Build document management UI components

This project successfully demonstrates modern full-stack development practices with cutting-edge AI integration, providing a solid foundation for the IPZS legislation assistant system.