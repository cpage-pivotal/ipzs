# Date Filtering Implementation Plan

## Overview

This plan outlines the implementation of date-aware filtering for the IPZS Legislative Assistant chat system. The goal is to ensure that when users select a reference date in the UI, the AI responses are based only on legislation that was effective on or before that date.

## Current Architecture Analysis

### Frontend State Management
- ✅ **DateSelectorComponent**: Already allows users to select reference dates
- ✅ **ChatService**: Has `dateContextSignal` that stores the selected date
- ✅ **ChatRequest**: Already includes `dateContext` field sent to backend

### Backend Processing
- ✅ **ChatController**: Receives `dateContext` in `ChatRequestDto`
- ⚠️ **SpringAIConfig**: Currently uses basic `QuestionAnswerAdvisor` without date filtering
- ✅ **Vector Store**: Documents include `effective_date` metadata

### Gap Analysis
**Missing**: The `dateContext` from the request is not being used to filter vector search results. The Spring AI advisor needs to be enhanced to respect the temporal context.

## Implementation Strategy

### Phase 1: Enhanced Vector Search with Date Filtering ✅ COMPLETED

#### 1.1 Create Custom Date-Aware Advisor ✅

**File**: `src/main/java/org/tanzu/ipzs/legislation/config/DateAwareQuestionAnswerAdvisor.java`

```java
@Component
public class DateAwareQuestionAnswerAdvisor implements BaseAdvisor {

    private final VectorStore vectorStore;
    private final PromptTemplate promptTemplate;
    private final SearchRequest defaultSearchRequest;
    private final Scheduler scheduler;
    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        LocalDate dateContext = extractDateContext(chatClientRequest.context());

        if (dateContext == null) {
            return performRegularQA(chatClientRequest);
        }

        // Search for relevant documents with date filtering
        List<Document> documents = searchWithDateFilter(userText, dateContext);

        // Filter documents in memory as additional safeguard
        List<Document> filteredDocuments = documents.stream()
                .filter(doc -> LegislationDateUtils.isDocumentEffectiveOnDate(doc, dateContext))
                .toList();

        // Build enhanced prompt with date context and filtered documents
        // Return updated request with augmented prompt
    }
}
```

**Key Features**:
- ✅ Implements Spring AI's BaseAdvisor interface
- ✅ Extracts date context from chat request parameters
- ✅ Builds metadata filter expressions for vector store
- ✅ Applies dual filtering: vector store + in-memory safeguard
- ✅ Enhanced prompt template with date awareness
- ✅ Maintains compatibility with existing chat flow
- ✅ Supports both regular QA and date-filtered modes

#### 1.2 Update Spring AI Configuration ✅

**File**: `src/main/java/org/tanzu/ipzs/legislation/config/SpringAIConfig.java`

```java
@Configuration
public class SpringAIConfig {

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder chatClientBuilder,
            DateAwareQuestionAnswerAdvisor dateAwareAdvisor) {

        return chatClientBuilder
                .defaultAdvisors(dateAwareAdvisor)
                .build();
    }
}
```

**Changes Made**:
- ✅ Replaced `QuestionAnswerAdvisor` with `DateAwareQuestionAnswerAdvisor`
- ✅ Updated dependency injection to use custom advisor
- ✅ Maintained clean configuration structure

#### 1.3 Enhance Chat Controller for Context Passing ✅

**File**: `src/main/java/org/tanzu/ipzs/legislation/controller/ChatController.java`

```java
@PostMapping
public ChatResponseDto chat(@RequestBody ChatRequestDto request) {
    String response = chatClient
            .prompt(request.message())
            .advisors(spec -> {
                if (request.dateContext() != null) {
                    spec.param("dateContext", request.dateContext());
                }
            })
            .call()
            .content();

    return new ChatResponseDto(
            response,
            request.sessionId() != null ? request.sessionId() : UUID.randomUUID(),
            List.of()
    );
}
```

**Changes Made**:
- ✅ Added conditional date context parameter passing
- ✅ Enhanced advisor specification with null safety
- ✅ Maintained existing response structure

### Phase 2: Vector Store Metadata Enhancement

#### 2.1 Standardize Date Metadata Format

**File**: `src/main/java/org/tanzu/ipzs/legislation/service/SampleDocumentService.java`

Ensure consistent date metadata:
```java
private List<Document> createVectorDocuments(DocumentTemplate template, String documentId) {
    // Current metadata enhancement
    metadata.put("effective_date", template.effectiveDate().toString()); // ISO format
    metadata.put("effective_date_epoch", template.effectiveDate().toEpochDay()); // For numeric filtering
    metadata.put("expiration_date", template.expirationDate()?.toString());
    metadata.put("is_current_as_of", (date) -> isEffectiveOnDate(template, date));
}
```

#### 2.2 Add Date Utility Methods ✅

**File**: `src/main/java/org/tanzu/ipzs/legislation/util/LegislationDateUtils.java`

```java
public class LegislationDateUtils {

    public static boolean isDocumentEffectiveOnDate(Document document, LocalDate queryDate) {
        if (queryDate == null) {
            return true; // No date filter applied
        }

        String effectiveDateStr = (String) document.getMetadata().get("effective_date");
        if (effectiveDateStr == null) {
            return true; // No effective date metadata, include document
        }

        try {
            LocalDate effectiveDate = LocalDate.parse(effectiveDateStr, ISO_DATE_FORMATTER);

            // Check if document is effective by the query date
            if (effectiveDate.isAfter(queryDate)) {
                return false;
            }

            // Check if document has expired by the query date
            String expirationDateStr = (String) document.getMetadata().get("expiration_date");
            if (expirationDateStr != null && !expirationDateStr.isEmpty()) {
                LocalDate expirationDate = LocalDate.parse(expirationDateStr, ISO_DATE_FORMATTER);
                return !expirationDate.isBefore(queryDate);
            }

            return true;
        } catch (DateTimeParseException e) {
            return true; // If date parsing fails, include the document
        }
    }

    public static String buildDateFilterExpression(LocalDate contextDate) {
        if (contextDate == null) {
            return null;
        }
        long epochDays = contextDate.toEpochDay();
        return "effective_date_epoch <= " + epochDays;
    }

    public static String formatDateForDisplay(LocalDate date) {
        if (date == null) {
            return "current date";
        }
        return date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }
}
```

**Features Implemented**:
- ✅ Document-based date filtering with null safety
- ✅ ISO date format parsing with error handling
- ✅ Expiration date support for temporal validity
- ✅ Vector store filter expression building
- ✅ Human-readable date formatting for prompts

### Phase 3: Advanced Context Management

#### 3.1 Create Date Context Service

**File**: `src/main/java/org/tanzu/ipzs/legislation/service/DateContextService.java`

```java
@Service
public class DateContextService {
    
    public List<Document> filterDocumentsByDate(List<Document> documents, LocalDate contextDate) {
        return documents.stream()
            .filter(doc -> isDocumentEffectiveOnDate(doc, contextDate))
            .collect(toList());
    }
    
    public String enhancePromptWithDateContext(String originalPrompt, LocalDate contextDate) {
        return String.format(
            "Context: Answer based on legislation effective as of %s.\n\nQuestion: %s",
            contextDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            originalPrompt
        );
    }
}
```

#### 3.2 Enhance System Prompts

Add temporal awareness to the AI system prompts:

```java
private static final String SYSTEM_PROMPT = """
    You are a legislative assistant for the Italian State Printing House (IPZS).
    
    CRITICAL: Only reference legislation that was effective on or before the specified context date: {contextDate}.
    If legislation has been superseded or expired by the context date, mention this explicitly.
    
    When multiple versions of legislation exist, always refer to the version that was in effect on the context date.
    """;
```

### Phase 4: Frontend Enhancements

#### 4.1 Update Chat Service for Better Date Handling

**File**: `src/main/frontend/src/app/core/services/chat.service.ts`

```typescript
export class ChatService {
  
  sendMessage(content: string): Observable<ChatResponse> {
    const enhancedContent = this.enhanceMessageWithDateContext(content);
    
    const request: ChatRequest = {
      message: enhancedContent,
      dateContext: this.dateContextSignal(),
      sessionId: this.sessionId
    };
    
    return this.http.post<ChatResponse>(this.baseUrl, request).pipe(
      tap(response => this.handleResponseWithDateValidation(response))
    );
  }
  
  private enhanceMessageWithDateContext(message: string): string {
    const date = this.dateContextSignal();
    return `[Context Date: ${date.toLocaleDateString('it-IT')}] ${message}`;
  }
}
```

#### 4.2 Add Date Context Indicator

**File**: `src/main/frontend/src/app/features/chat/chat-messages/chat-messages.component.html`

```html
<div class="date-context-indicator">
  <mat-chip>
    <mat-icon>event</mat-icon>
    {{ translationService.t('chat.activeDate') }}: 
    {{ chatService.dateContext() | date:'longDate':'it-IT' }}
  </mat-chip>
</div>
```

### Phase 5: Testing Strategy

#### 5.1 Unit Tests

**Backend Tests**:
```java
@Test
void shouldFilterDocumentsByEffectiveDate() {
    // Test date filtering logic
    // Verify only documents effective before context date are returned
}

@Test
void shouldHandleSupersededDocuments() {
    // Test that superseded documents are properly handled
    // Verify correct version is selected based on date
}
```

**Frontend Tests**:
```typescript
describe('ChatService Date Filtering', () => {
  it('should include date context in requests', () => {
    // Test that dateContext is properly sent to backend
  });
  
  it('should update responses when date context changes', () => {
    // Test reactive behavior when date changes
  });
});
```

#### 5.2 Integration Tests

**Scenario Testing**:
1. **Speed Limit Evolution**: Query about speed limits with dates before/after 2025 supersession
2. **Cannabis Legalization**: Test progression from Schedule III (2024) to full legalization (2025)
3. **Document Availability**: Ensure future documents don't appear in past date queries

#### 5.3 Test Data Scenarios

```java
// Test documents with clear temporal relationships
DocumentTemplate speedLimit2024 = new DocumentTemplate(
    "Speed Limit Act 2024",
    content,
    "Federal Legislation",
    LocalDate.of(2024, 1, 1),    // effective
    LocalDate.of(2023, 12, 15),  // published
    "Congress",
    "H.R. 2024-001",
    List.of("75 mph limit")
);

DocumentTemplate speedLimit2025 = new DocumentTemplate(
    "Speed Limit Act 2025",
    content,
    "Federal Legislation", 
    LocalDate.of(2025, 9, 1),    // effective
    LocalDate.of(2025, 8, 15),   // published
    "Congress",
    "H.R. 2025-042",
    List.of("85 mph autonomous", "supersedes H.R. 2024-001")
);
```

## Implementation Timeline

### Week 1: Core Infrastructure ✅ COMPLETED
- ✅ Implement `DateAwareQuestionAnswerAdvisor`
- ✅ Update `SpringAIConfig` with new advisor
- ✅ Implement date filtering utilities (`LegislationDateUtils`)

### Week 2: Service Layer (CURRENT PHASE)
- [ ] Enhance vector document metadata with date fields
- [ ] Create `DateContextService` (optional - functionality integrated into advisor)
- ✅ Update `ChatController` to pass date context
- [ ] Test integration with sample documents

### Week 3: Frontend Integration (NEXT)
- [ ] Update `ChatService` with enhanced date handling
- [ ] Add date context indicators to UI
- [ ] Update translation keys for new features

### Week 4: Testing & Refinement (FINAL)
- [ ] Comprehensive unit and integration tests
- [ ] User acceptance testing with temporal scenarios
- [ ] Performance optimization for filtered searches

## Technical Considerations

### Performance Optimization
- **Index Strategy**: Ensure `effective_date_epoch` metadata is efficiently indexed in pgvector
- **Caching**: Consider caching filtered document sets for common date ranges
- **Batch Processing**: Optimize vector searches with date filters

### Error Handling
- **Invalid Dates**: Handle edge cases where context date is in the future
- **Missing Metadata**: Graceful degradation when documents lack date metadata
- **Supersession Chains**: Handle complex document supersession relationships

### Monitoring
- **Search Metrics**: Track effectiveness of date-filtered searches
- **User Patterns**: Monitor how users interact with date context features
- **Performance Metrics**: Measure impact of filtering on response times

## Success Criteria

1. **Accuracy**: AI responses reflect only legislation effective on the selected date
2. **User Experience**: Seamless date selection with clear feedback
3. **Performance**: Date filtering adds < 100ms to response times
4. **Reliability**: Robust handling of complex temporal relationships
5. **Multilingual**: Full Italian/English support for date-related features

## Future Enhancements

- **Smart Date Suggestions**: Recommend relevant dates based on query content
- **Timeline View**: Visual representation of legislation evolution
- **Comparative Analysis**: Side-by-side comparison of legislation across dates
- **Notification System**: Alert users when querying about superseded legislation