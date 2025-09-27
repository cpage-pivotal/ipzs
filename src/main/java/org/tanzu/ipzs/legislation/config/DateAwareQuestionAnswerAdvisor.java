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

        CRITICAL: Only reference legislation that was effective on or before {date_context}.
        If legislation has been superseded or expired by this date, mention this explicitly.

        When multiple versions of legislation exist, always refer to the version that was in effect on the specified date.

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

    // Primary constructor for Spring dependency injection
    @Autowired
    public DateAwareQuestionAnswerAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.defaultSearchRequest = SearchRequest.builder().topK(5).similarityThreshold(0.7).build();
        this.promptTemplate = DEFAULT_PROMPT_TEMPLATE;
        this.scheduler = DEFAULT_SCHEDULER;
        this.order = 0;
    }

    // Constructor for programmatic configuration
    public DateAwareQuestionAnswerAdvisor(VectorStore vectorStore, int topK, double threshold) {
        this.vectorStore = vectorStore;
        this.defaultSearchRequest = SearchRequest.builder().topK(topK).similarityThreshold(threshold).build();
        this.promptTemplate = DEFAULT_PROMPT_TEMPLATE;
        this.scheduler = DEFAULT_SCHEDULER;
        this.order = 0;
    }

    // Private constructor for builder pattern
    private DateAwareQuestionAnswerAdvisor(VectorStore vectorStore,
                                           SearchRequest searchRequest,
                                           PromptTemplate promptTemplate,
                                           Scheduler scheduler,
                                           int order) {
        this.vectorStore = vectorStore;
        this.defaultSearchRequest = searchRequest;
        this.promptTemplate = promptTemplate;
        this.scheduler = scheduler;
        this.order = order;
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
        LocalDate dateContext = extractDateContext(chatClientRequest.context());

        if (dateContext == null) {
            // No date context provided, fall back to regular QA behavior
            return performRegularQA(chatClientRequest);
        }

        // Get the user message text
        UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
        String userText = userMessage.getText();

        // Search for relevant documents with date filtering
        List<Document> documents = searchWithDateFilter(userText, dateContext);

        // Filter documents in memory as an additional safeguard
        List<Document> filteredDocuments = documents.stream()
                .filter(doc -> LegislationDateUtils.isDocumentEffectiveOnDate(doc, dateContext))
                .toList();

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

        // Return updated request with augmented prompt and context
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .context(context)
                .build();
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

    private LocalDate extractDateContext(Map<String, Object> context) {
        if (context != null && context.containsKey(DATE_CONTEXT_PARAM)) {
            Object dateValue = context.get(DATE_CONTEXT_PARAM);
            if (dateValue instanceof LocalDate localDate) {
                return localDate;
            }
        }
        return null;
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
        formatted.append(document.getText());

        // Add metadata context
        Map<String, Object> metadata = document.getMetadata();
        if (metadata != null) {
            String title = (String) metadata.get("title");
            String effectiveDate = (String) metadata.get("effective_date");
            String documentType = (String) metadata.get("document_type");

            if (title != null) {
                formatted.append("\nTitle: ").append(title);
            }
            if (effectiveDate != null) {
                formatted.append("\nEffective Date: ").append(effectiveDate);
            }
            if (documentType != null) {
                formatted.append("\nType: ").append(documentType);
            }
        }

        return formatted.toString();
    }

    // Builder pattern for more flexible construction
    public static Builder builder(VectorStore vectorStore) {
        return new Builder(vectorStore);
    }

    public static class Builder {
        private final VectorStore vectorStore;
        private SearchRequest searchRequest = SearchRequest.builder().topK(5).similarityThreshold(0.7).build();
        private PromptTemplate promptTemplate = DEFAULT_PROMPT_TEMPLATE;
        private Scheduler scheduler = DEFAULT_SCHEDULER;
        private int order = 0;

        private Builder(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
        }

        public Builder searchRequest(SearchRequest searchRequest) {
            this.searchRequest = searchRequest;
            return this;
        }

        public Builder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public DateAwareQuestionAnswerAdvisor build() {
            return new DateAwareQuestionAnswerAdvisor(vectorStore, searchRequest, promptTemplate, scheduler, order);
        }
    }
}