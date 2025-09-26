# Legislation RAG Chatbot - Design & Implementation Plan

## Project Overview

This document outlines the design and implementation plan for a legislation knowledge management system for **Istituto Poligrafico e Zecca dello Stato** (IPZS). The system leverages Retrieval Augmented Generation (RAG) patterns with Spring AI 1.1.0-M2 to enable intelligent querying of Italian legislation through a conversational interface.

## Executive Summary

The system provides:
- Intelligent legislation search and retrieval using vector similarity
- Temporal context awareness for historically accurate queries
- Natural language interaction through a chatbot interface
- Cloud-native deployment on Cloud Foundry
- Scalable document ingestion and processing pipeline

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
- **Angular 20** - Modern frontend framework with signals
- **Angular Material 20** - Material Design 3 components
- **TypeScript 5.7** - Type-safe JavaScript
- **SCSS** - Styling with Material Design tokens
- **RxJS 7** - Reactive programming

### Infrastructure & Deployment
- **Cloud Foundry** - PaaS platform
- **Maven 3.9+** - Build automation
- **Docker** - Containerization (for local development)
- **GitHub Actions** - CI/CD pipeline

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

### Package Structure

```
org.tanzu.ipzs.legislation
├── config/
│   ├── SpringAIConfig.java
│   ├── VectorStoreConfig.java
│   └── CloudFoundryConfig.java
├── controller/
│   ├── ChatController.java
│   ├── DocumentController.java
│   └── AdminController.java
├── service/
│   ├── ChatService.java
│   ├── DocumentIngestionService.java
│   ├── VectorSearchService.java
│   └── RAGService.java
├── repository/
│   ├── LegislationDocumentRepository.java
│   ├── DocumentChunkRepository.java
│   └── ChatSessionRepository.java
├── model/
│   ├── dto/
│   │   ├── ChatRequestDto.java (record)
│   │   ├── ChatResponseDto.java (record)
│   │   └── DocumentMetadataDto.java (record)
│   └── entity/
│       ├── LegislationDocument.java
│       ├── DocumentChunk.java
│       └── ChatSession.java
├── vectorstore/
│   ├── PgVectorStore.java
│   └── MetadataFilter.java
└── util/
    ├── DocumentParser.java
    └── ChunkingStrategy.java
```

### Key Spring AI Components

#### 1. Vector Store Configuration

```java
@Configuration
public class VectorStoreConfig {
    
    @Bean
    public PgVectorStore vectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel) {
        return PgVectorStore.builder()
            .jdbcTemplate(jdbcTemplate)
            .embeddingModel(embeddingModel)
            .tableName("document_chunks")
            .dimension(1536)
            .build();
    }
    
    @Bean
    public EmbeddingModel embeddingModel() {
        return new OpenAiEmbeddingModel(
            OpenAiApi.builder()
                .apiKey(apiKey)
                .build());
    }
}
```

#### 2. RAG Service with Date Filtering

```java
@Service
public class RAGService {
    
    public ChatResponse processQuery(
            String query, 
            LocalDate dateContext) {
        
        // Create metadata filter for date context
        Filter dateFilter = FilterExpressionBuilder
            .builder()
            .lte("effective_date", dateContext)
            .and(filter -> filter
                .gt("expiration_date", dateContext)
                .or()
                .isNull("expiration_date"))
            .build();
        
        // Perform similarity search with filter
        List<Document> relevantDocs = vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(5)
                .withSimilarityThreshold(0.7)
                .withFilterExpression(dateFilter)
        );
        
        // Generate response using RAG
        return chatModel.call(
            new Prompt(
                createPromptTemplate(query, relevantDocs),
                ChatOptions.builder()
                    .temperature(0.7)
                    .build()
            )
        );
    }
}
```

### Document Ingestion Pipeline

1. **Document Upload**: Accept PDF/DOCX/HTML formats
2. **Text Extraction**: Use Apache Tika for content extraction
3. **Chunking**: Split documents into semantic chunks (500-1000 tokens)
4. **Metadata Extraction**: Parse dates, authorities, document types
5. **Embedding Generation**: Create vector embeddings via OpenAI
6. **Storage**: Save chunks with embeddings to PgVector

## Frontend Implementation

### Angular Application Structure

```
src/main/frontend/src/app/
├── core/
│   ├── services/
│   │   ├── chat.service.ts
│   │   ├── document.service.ts
│   │   └── date-context.service.ts
│   └── models/
│       ├── chat-message.model.ts
│       └── document.model.ts
├── features/
│   ├── chat/
│   │   ├── chat-container/
│   │   ├── chat-input/
│   │   ├── chat-messages/
│   │   └── date-selector/
│   └── admin/
│       ├── document-upload/
│       └── document-list/
├── shared/
│   ├── components/
│   │   ├── loading-spinner/
│   │   └── error-dialog/
│   └── material/
│       └── material.module.ts
└── app.component.ts
```

### Key Frontend Components

#### 1. Chat Service with Signals

```typescript
@Injectable({ providedIn: 'root' })
export class ChatService {
  private messagesSignal = signal<ChatMessage[]>([]);
  private dateContextSignal = signal<Date>(new Date());
  
  public messages = computed(() => this.messagesSignal());
  public dateContext = computed(() => this.dateContextSignal());
  
  sendMessage(content: string): Observable<ChatResponse> {
    const request: ChatRequest = {
      message: content,
      dateContext: this.dateContextSignal(),
      sessionId: this.sessionId
    };
    
    return this.http.post<ChatResponse>(
      '/api/chat', 
      request
    ).pipe(
      tap(response => {
        this.messagesSignal.update(messages => [
          ...messages,
          { type: 'user', content },
          { type: 'assistant', content: response.message }
        ]);
      })
    );
  }
}
```

#### 2. Material Design Chat Interface

```typescript
@Component({
  selector: 'app-chat-container',
  standalone: true,
  imports: [CommonModule, MaterialModule],
  template: `
    <mat-card class="chat-container">
      <mat-card-header>
        <mat-card-title>Assistente Legislativo IPZS</mat-card-title>
        <app-date-selector 
          [currentDate]="dateContext()"
          (dateChange)="onDateChange($event)">
        </app-date-selector>
      </mat-card-header>
      
      <mat-card-content class="chat-messages">
        <app-chat-messages 
          [messages]="messages()">
        </app-chat-messages>
      </mat-card-content>
      
      <mat-card-actions>
        <app-chat-input 
          (sendMessage)="onSendMessage($event)"
          [disabled]="isLoading()">
        </app-chat-input>
      </mat-card-actions>
    </mat-card>
  `
})
export class ChatContainerComponent {
  messages = this.chatService.messages;
  dateContext = this.chatService.dateContext;
  isLoading = signal(false);
  
  constructor(private chatService: ChatService) {}
}
```

## Cloud Foundry Deployment

### manifest.yml

```yaml
applications:
  - name: ipzs-legislation-assistant
    memory: 2G
    instances: 2
    buildpack: java_buildpack
    path: target/legislation-assistant-1.0.0.jar
    env:
      JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 21.+ } }'
      SPRING_PROFILES_ACTIVE: cloud
    services:
      - postgres-pgvector
      - openai-service
    routes:
      - route: legislation-assistant.apps.cf.ipzs.it
```

### Application Properties

```properties
# Cloud Foundry Profile
spring.profiles.active=cloud

# Database Configuration
spring.datasource.url=${vcap.services.postgres-pgvector.credentials.uri}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# Spring AI Configuration
spring.ai.openai.api-key=${vcap.services.openai-service.credentials.api-key}
spring.ai.openai.embedding.model=text-embedding-ada-002
spring.ai.openai.chat.model=gpt-4-turbo

# Vector Store Configuration
spring.ai.vectorstore.pgvector.dimension=1536
spring.ai.vectorstore.pgvector.similarity-threshold=0.7
```

## Implementation Phases

### Phase 1: Foundation (Weeks 1-2)
- Set up project structure with Spring AI 1.1.0-M2
- Configure PostgreSQL with PgVector extension
- Implement basic Spring Boot REST endpoints
- Create Angular project structure with Material Design

### Phase 2: Data Layer (Weeks 3-4)
- Design and implement database schema
- Create JPA entities and repositories
- Implement document ingestion service
- Set up text extraction and chunking pipeline

### Phase 3: Vector Store Integration (Weeks 5-6)
- Configure PgVector with Spring AI
- Implement embedding generation service
- Create vector search functionality
- Add metadata filtering for date context

### Phase 4: RAG Implementation (Weeks 7-8)
- Integrate OpenAI/Azure OpenAI for chat completion
- Implement RAG service with prompt engineering
- Add conversation context management
- Create date-aware query filtering

### Phase 5: Frontend Development (Weeks 9-10)
- Build Angular chat interface with Material Design
- Implement date selector component
- Create admin interface for document upload
- Add real-time chat functionality

### Phase 6: Testing & Optimization (Weeks 11-12)
- Unit and integration testing
- Performance optimization for vector searches
- UI/UX refinement
- Security audit and improvements

### Phase 7: Deployment (Week 13)
- Cloud Foundry deployment configuration
- CI/CD pipeline setup
- Production monitoring configuration
- Documentation and training

## Key Considerations

### Performance Optimization
- Implement connection pooling for database
- Use async processing for document ingestion
- Cache frequently accessed legislation
- Optimize chunk size for retrieval accuracy

### Security
- Implement authentication/authorization
- Encrypt sensitive data at rest
- Use secure communication (HTTPS)
- Implement rate limiting for API endpoints

### Scalability
- Design for horizontal scaling on Cloud Foundry
- Use message queues for document processing
- Implement database read replicas if needed
- Consider CDN for static content

### Monitoring & Observability
- Application metrics with Micrometer
- Distributed tracing with Spring Cloud Sleuth
- Log aggregation with ELK stack
- Custom dashboards for RAG performance

## Success Metrics

1. **Query Accuracy**: >85% relevant results in top 5 retrievals
2. **Response Time**: <2 seconds for typical queries
3. **System Availability**: 99.5% uptime
4. **User Satisfaction**: >4.0/5.0 rating
5. **Document Coverage**: Support for 95% of legislation formats

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Large document volumes affecting performance | High | Implement efficient chunking and indexing strategies |
| Complex legal language affecting accuracy | Medium | Fine-tune embeddings and prompts for legal domain |
| Date filtering complexity | Medium | Comprehensive testing with historical scenarios |
| Cloud Foundry resource limits | Low | Monitor and scale instances as needed |

## Future Enhancements

1. **Multi-language Support**: Add support for EU legislation in multiple languages
2. **Advanced Analytics**: Track most queried topics and user patterns
3. **Document Relationships**: Link related legislation and amendments
4. **Export Functionality**: Generate reports and summaries
5. **Voice Interface**: Add speech-to-text capabilities
6. **Mobile Application**: Native iOS/Android apps

## Conclusion

This implementation plan provides a comprehensive roadmap for building a sophisticated legislation query system using modern RAG patterns with Spring AI. The combination of vector search, temporal filtering, and conversational AI will enable IPZS to provide intelligent access to Italian legislation while maintaining historical accuracy through date context awareness.