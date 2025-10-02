package org.tanzu.ipzs.legislation.config;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.tanzu.ipzs.legislation.util.LegislationDateUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DateAwareQuestionAnswerAdvisor implements BaseAdvisor {

    private static final String DATE_CONTEXT_PARAM = "dateContext";
    private static final String RETRIEVED_DOCUMENTS = "date_aware_retrieved_documents";
    private static final String FILTER_EXPRESSION = "date_aware_filter_expression";

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
        You are a legislative assistant for the Italian State Printing House (IPZS).

        CRITICAL: Today's date is {date_context}. You must answer based ONLY on legislation that was in effect on {date_context}.

        TEMPORAL LOGIC RULES (READ CAREFULLY):
        1. A law is APPLICABLE on {date_context} if its effective date is {date_context} or earlier
        2. A law is NOT APPLICABLE on {date_context} if its effective date is after {date_context}
        3. If a newer law supersedes an older law, and the newer law is effective by {date_context}, then use ONLY the newer law
        4. ALWAYS compare dates carefully: September 1, 2025 comes BEFORE September 10, 2025

        EXAMPLE DATE LOGIC:
        - If effective date is "January 1, 2024" and query date is "September 10, 2025" → APPLICABLE (Jan 1, 2024 is before Sep 10, 2025)
        - If effective date is "September 1, 2025" and query date is "September 10, 2025" → APPLICABLE (Sep 1, 2025 is before Sep 10, 2025)  
        - If effective date is "October 1, 2025" and query date is "September 10, 2025" → NOT APPLICABLE (Oct 1, 2025 is after Sep 10, 2025)

        SUPERSESSION LOGIC:
        - If you see multiple laws on the same topic, check which one was most recently effective by {date_context}
        - If a law states it "supersedes" another law, and the superseding law is effective by {date_context}, ignore the old law
        - Always use the most current version that was effective by {date_context}

        RESPONSE FORMAT:
        - Start with: "As of {date_context}, the current legislation in effect is..."
        - Reference only the most current applicable law
        - If legislation changed during the timeframe, explain what was superseded and when
        - Use present tense for laws that are in effect on {date_context}

        {query}

        Context information is below, surrounded by ---------------------

        ---------------------
        {question_answer_context}
        ---------------------

        Given the context and provided history information and not prior knowledge,
        reply to the user comment. If the answer is not in the context, inform
        the user that you can't answer the question.
        """);

    private final VectorStore vectorStore;
    private final PromptTemplate promptTemplate;
    private final SearchRequest defaultSearchRequest;
    private final Scheduler scheduler;
    private final int order;

    // Store the date context as instance variable set by Spring AI
    private final ThreadLocal<LocalDate> threadLocalDateContext = new ThreadLocal<>();

    // Primary constructor for Spring dependency injection
    @Autowired
    public DateAwareQuestionAnswerAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.defaultSearchRequest = SearchRequest.builder().topK(5).similarityThreshold(0.4).build();
        this.promptTemplate = DEFAULT_PROMPT_TEMPLATE;
        this.scheduler = DEFAULT_SCHEDULER;
        this.order = 0;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        // Try multiple ways to extract the date context
        LocalDate dateContext = extractDateContext(chatClientRequest);

        // Debug logging
        System.out.println("DEBUG: Extracted date context: " + dateContext);
        System.out.println("DEBUG: Request context keys: " + chatClientRequest.context().keySet());

        if (dateContext == null) {
            // No date context provided, fall back to regular QA behavior
            System.out.println("DEBUG: No date context found, using regular QA");
            return performRegularQA(chatClientRequest);
        }

        // Get the user message text
        UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
        String userText = userMessage.getText();

        System.out.println("DEBUG: Processing with date context: " + dateContext + " for query: " + userText);

        // Search for relevant documents with date filtering
        List<Document> documents = searchWithDateFilter(userText, dateContext);

        System.out.println("DEBUG: Retrieved " + documents.size() + " documents from vector search");

        // Filter documents in memory as an additional safeguard
        List<Document> filteredDocuments = documents.stream()
                .filter(doc -> {
                    boolean isEffective = LegislationDateUtils.isDocumentEffectiveOnDate(doc, dateContext);
                    String title = (String) doc.getMetadata().get("title");
                    String effectiveDate = (String) doc.getMetadata().get("effective_date");
                    System.out.println("DEBUG: Document '" + title + "' (effective: " + effectiveDate + ") - " +
                            (isEffective ? "INCLUDED" : "EXCLUDED"));
                    return isEffective;
                })
                .sorted((doc1, doc2) -> {
                    // Sort by effective date descending (most recent first)
                    String date1 = (String) doc1.getMetadata().get("effective_date");
                    String date2 = (String) doc2.getMetadata().get("effective_date");
                    if (date1 != null && date2 != null) {
                        try {
                            LocalDate localDate1 = LocalDate.parse(date1);
                            LocalDate localDate2 = LocalDate.parse(date2);
                            return localDate2.compareTo(localDate1); // Reverse order (newest first)
                        } catch (Exception e) {
                            // If date parsing fails, maintain original order
                            return 0;
                        }
                    }
                    return 0;
                })
                .toList();

        System.out.println("DEBUG: After filtering, " + filteredDocuments.size() + " documents remain");

        // Debug: Show the order of documents after sorting
        filteredDocuments.forEach(doc -> {
            String title = (String) doc.getMetadata().get("title");
            String effectiveDate = (String) doc.getMetadata().get("effective_date");
            System.out.println("DEBUG: Sorted order - '" + title + "' (effective: " + effectiveDate + ")");
        });

        // Create context for the response metadata
        Map<String, Object> context = new HashMap<>(chatClientRequest.context());
        context.put(RETRIEVED_DOCUMENTS, filteredDocuments);

        // Build document context
        String documentContext = filteredDocuments.isEmpty() ?
                "No relevant legislation found for the specified date." :
                filteredDocuments.stream()
                        .map(this::formatDocumentContent)
                        .collect(Collectors.joining(System.lineSeparator()));

        // Create enhanced prompt with date context
        String augmentedUserText = this.promptTemplate.render(Map.of(
                "query", userText,
                "question_answer_context", documentContext,
                "date_context", LegislationDateUtils.formatDateForDisplay(dateContext)
        ));

        System.out.println("DEBUG: Enhanced prompt created with " + filteredDocuments.size() + " documents");

        // Return updated request with augmented prompt and context
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .context(context)
                .build();
    }

    private LocalDate extractDateContext(ChatClientRequest chatClientRequest) {
        // Method 1: Check request context (this is where we expect it)
        if (chatClientRequest.context().containsKey(DATE_CONTEXT_PARAM)) {
            Object dateValue = chatClientRequest.context().get(DATE_CONTEXT_PARAM);
            if (dateValue instanceof LocalDate localDate) {
                return localDate;
            }
            if (dateValue instanceof String dateString) {
                try {
                    return LocalDate.parse(dateString);
                } catch (Exception e) {
                    System.err.println("Failed to parse date string: " + dateString);
                }
            }
        }

        // Method 2: Check if it's stored in thread local (for parameter passing)
        return threadLocalDateContext.get();
    }

    // Method to set date context (can be called by Spring AI parameter injection)
    public void setDateContext(LocalDate dateContext) {
        this.threadLocalDateContext.set(dateContext);
    }

    private ChatClientRequest performRegularQA(ChatClientRequest chatClientRequest) {
        // Regular QA behavior without date filtering
        UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
        String userText = userMessage.getText();

        // Search without date constraints
        SearchRequest searchRequest = SearchRequest.from(this.defaultSearchRequest)
                .query(userText)
                .filterExpression(getFilterExpression(chatClientRequest.context()))
                .build();

        List<Document> documents = this.vectorStore.similaritySearch(searchRequest);

        Map<String, Object> context = new HashMap<>(chatClientRequest.context());
        context.put(RETRIEVED_DOCUMENTS, documents);

        String documentContext = documents.isEmpty() ?
                "" :
                documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining(System.lineSeparator()));

        // Use a simplified template for regular QA
        PromptTemplate regularTemplate = new PromptTemplate("""
            {query}

            Context information is below, surrounded by ---------------------

            ---------------------
            {question_answer_context}
            ---------------------

            Given the context and provided history information and not prior knowledge,
            reply to the user comment. If the answer is not in the context, inform
            the user that you can't answer the question.
            """);

        String augmentedUserText = regularTemplate.render(Map.of(
                "query", userText,
                "question_answer_context", documentContext
        ));

        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .context(context)
                .build();
    }

    private List<Document> searchWithDateFilter(String query, LocalDate dateContext) {
        SearchRequest.Builder searchRequestBuilder = SearchRequest.from(this.defaultSearchRequest)
                .query(query);

        // Add date filter if possible
        String filterExpression = LegislationDateUtils.buildDateFilterExpression(dateContext);
        if (filterExpression != null) {
            try {
                Filter.Expression filter = new FilterExpressionTextParser().parse(filterExpression);
                searchRequestBuilder.filterExpression(filter);
                System.out.println("DEBUG: Applied vector store filter: " + filterExpression);
            } catch (Exception e) {
                // If vector store doesn't support the filter format, we'll rely on in-memory filtering
                System.err.println("Vector store filter not supported, using in-memory filtering: " + e.getMessage());
            }
        }

        return this.vectorStore.similaritySearch(searchRequestBuilder.build());
    }

    private Filter.Expression getFilterExpression(Map<String, Object> context) {
        if (context != null && context.containsKey(FILTER_EXPRESSION)
                && StringUtils.hasText(context.get(FILTER_EXPRESSION).toString())) {
            return new FilterExpressionTextParser().parse(context.get(FILTER_EXPRESSION).toString());
        }
        return this.defaultSearchRequest.getFilterExpression();
    }

    private String formatDocumentContent(Document document) {
        StringBuilder formatted = new StringBuilder();

        // Add metadata context FIRST to make it more prominent
        Map<String, Object> metadata = document.getMetadata();
        String title = (String) metadata.get("title");
        String effectiveDate = (String) metadata.get("effective_date");
        String documentType = (String) metadata.get("document_type");
        String documentNumber = (String) metadata.get("document_number");

        formatted.append("=== DOCUMENT METADATA ===\n");
        if (title != null) {
            formatted.append("Title: ").append(title).append("\n");
        }
        if (effectiveDate != null) {
            formatted.append("Effective Date: ").append(effectiveDate).append("\n");
        }
        if (documentType != null) {
            formatted.append("Type: ").append(documentType).append("\n");
        }
        if (documentNumber != null) {
            formatted.append("Document Number: ").append(documentNumber).append("\n");
        }
        formatted.append("=== DOCUMENT CONTENT ===\n");

        formatted.append(document.getText());
        formatted.append("\n=== END DOCUMENT ===\n");

        return formatted.toString();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // Add retrieved documents to response metadata
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        } else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }

        chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS,
                chatClientResponse.context().get(RETRIEVED_DOCUMENTS));

        return ChatClientResponse.builder()
                .chatResponse(chatResponseBuilder.build())
                .context(chatClientResponse.context())
                .build();
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        return Mono.just(chatClientRequest)
                .publishOn(this.getScheduler())
                .map(request -> this.before(request, streamAdvisorChain))
                .flatMapMany(streamAdvisorChain::nextStream)
                .map(response -> this.after(response, streamAdvisorChain));
    }
}