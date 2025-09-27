package org.tanzu.ipzs.legislation.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tanzu.ipzs.legislation.model.entity.LegislationDocument;
import org.tanzu.ipzs.legislation.repository.LegislationDocumentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
public class SampleDocumentService {

    @Autowired
    private LegislationDocumentRepository documentRepository;

    @Autowired
    private VectorStore vectorStore;

    private final TextSplitter textSplitter = new TokenTextSplitter(500, 100, 5, 10000, true);

    // Modern Java record for document templates
    public record DocumentTemplate(
            String title,
            String content,
            String documentType,
            LocalDate effectiveDate,
            LocalDate publicationDate,
            String issuingAuthority,
            String documentNumber,
            List<String> keyProvisions
    ) {}

    public record IngestionResult(
            String documentId,
            int chunksCreated,
            boolean success,
            String message
    ) {}

    @Transactional
    public CompletableFuture<List<IngestionResult>> generateAndIngestSampleDocuments() {
        return CompletableFuture.supplyAsync(() -> {
            var templates = createDocumentTemplates();
            return templates.stream()
                    .map(this::processDocumentTemplate)
                    .toList();
        });
    }

    private IngestionResult processDocumentTemplate(DocumentTemplate template) {
        try {
            var documentId = generateDocumentId(template);

            // Check if document already exists
            if (documentRepository.existsByDocumentId(documentId)) {
                return new IngestionResult(
                        documentId,
                        0,
                        false,
                        "Document already exists: " + template.title()
                );
            }

            // Create and save legislation document (metadata only)
            var document = createLegislationDocument(template);
            documentRepository.save(document);

            // Create vector documents and add directly to vector store
            var vectorDocuments = createVectorDocuments(template, documentId);
            vectorStore.add(vectorDocuments);

            return new IngestionResult(
                    documentId,
                    vectorDocuments.size(),
                    true,
                    "Successfully ingested document: " + template.title()
            );
        } catch (Exception e) {
            return new IngestionResult(
                    generateDocumentId(template),
                    0,
                    false,
                    "Failed to ingest document: " + e.getMessage()
            );
        }
    }

    private LegislationDocument createLegislationDocument(DocumentTemplate template) {
        var document = new LegislationDocument();
        document.setDocumentId(generateDocumentId(template));
        document.setTitle(template.title());
        document.setDocumentType(template.documentType());
        document.setPublicationDate(template.publicationDate());
        document.setEffectiveDate(template.effectiveDate());
        document.setIssuingAuthority(template.issuingAuthority());
        document.setDocumentNumber(template.documentNumber());
        document.setRawContent(template.content());
        return document;
    }

    /**
     * Create vector documents with rich metadata for semantic search
     */
    private List<Document> createVectorDocuments(DocumentTemplate template, String documentId) {
        // Create a Document object for Spring AI
        var springAiDocument = new Document(template.content());

        // Split the document using Spring AI TextSplitter
        var splitDocuments = textSplitter.split(List.of(springAiDocument));

        return IntStream.range(0, splitDocuments.size())
                .mapToObj(i -> {
                    var splitDoc = splitDocuments.get(i);

                    // Rich metadata for effective semantic search and filtering
                    var metadata = new java.util.HashMap<String, Object>();

                    // Document identification
                    metadata.put("document_id", documentId);
                    metadata.put("title", template.title());
                    metadata.put("document_number", template.documentNumber());

                    // Classification and authority
                    metadata.put("document_type", template.documentType());
                    metadata.put("issuing_authority", template.issuingAuthority());

                    // Temporal context (critical for date-aware queries)
                    metadata.put("effective_date", template.effectiveDate().toString());
                    metadata.put("publication_date", template.publicationDate().toString());
                    metadata.put("effective_year", template.effectiveDate().getYear());
                    metadata.put("effective_month", template.effectiveDate().getMonthValue());

                    // Chunking information
                    metadata.put("chunk_index", i);
                    metadata.put("total_chunks", splitDocuments.size());

                    // Semantic content for enhanced search
                    metadata.put("key_provisions", String.join(", ", template.keyProvisions()));
                    metadata.put("subject_area", inferSubjectArea(template.title()));

                    // For supersession relationships
                    metadata.put("is_current", isCurrentDocument(template.effectiveDate()));
                    metadata.put("generation", template.effectiveDate().getYear() >= 2025 ? "second" : "first");

                    return new Document(splitDoc.getText(), metadata);
                })
                .toList();
    }

    /**
     * Infer subject area from document title for better categorization
     */
    private String inferSubjectArea(String title) {
        var lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("speed") || lowerTitle.contains("highway")) return "transportation";
        if (lowerTitle.contains("cannabis") || lowerTitle.contains("drug") || lowerTitle.contains("substance")) return "drug_policy";
        if (lowerTitle.contains("immigration") || lowerTitle.contains("border") || lowerTitle.contains("visa")) return "immigration";
        if (lowerTitle.contains("airline") || lowerTitle.contains("baggage") || lowerTitle.contains("travel")) return "aviation";
        if (lowerTitle.contains("park") || lowerTitle.contains("naming")) return "parks";
        return "general";
    }

    /**
     * Determine if document is currently effective
     */
    private boolean isCurrentDocument(LocalDate effectiveDate) {
        return effectiveDate.isBefore(LocalDate.now().plusDays(1)); // Include today
    }

    private String generateDocumentId(DocumentTemplate template) {
        return template.documentNumber().replace(".", "-").toLowerCase();
    }

    private List<DocumentTemplate> createDocumentTemplates() {
        return List.of(
                // Speed Limits - 2024
                new DocumentTemplate(
                        "Highway Speed Limit Modernization Act of 2024",
                        createSpeedLimitAct2024(),
                        "Federal Legislation",
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2023, 12, 15),
                        "United States Congress",
                        "H.R. 2024-001",
                        List.of("75 mph rural interstate", "65 mph urban highway", "25 mph residential")
                ),

                // Speed Limits - 2025
                new DocumentTemplate(
                        "Automated Vehicle Speed Integration Act of 2025",
                        createSpeedLimitAct2025(),
                        "Federal Legislation",
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 8, 15),
                        "United States Congress",
                        "H.R. 2025-042",
                        List.of("85 mph autonomous vehicles", "dynamic speed zones", "supersedes 2024 Act")
                ),

                // Drug Policy - 2024
                new DocumentTemplate(
                        "Controlled Substance Reform Act of 2024",
                        createDrugPolicyAct2024(),
                        "Federal Legislation",
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2023, 12, 20),
                        "Drug Enforcement Administration",
                        "DEA-2024-001",
                        List.of("Schedule III marijuana", "medical use allowed", "federal licensing required")
                ),

                // Drug Policy - 2025
                new DocumentTemplate(
                        "Cannabis Legalization and Regulation Act of 2025",
                        createDrugPolicyAct2025(),
                        "Federal Legislation",
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 8, 10),
                        "United States Congress",
                        "H.R. 2025-089",
                        List.of("full legalization", "federal taxation", "expungement program")
                ),

                // Immigration Law - 2024
                new DocumentTemplate(
                        "Border Security Enhancement Act of 2024",
                        createImmigrationAct2024(),
                        "Federal Legislation",
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2023, 12, 22),
                        "Department of Homeland Security",
                        "DHS-2024-003",
                        List.of("biometric verification", "2-year visitor visas", "15% H-1B increase")
                ),

                // Immigration Law - 2025
                new DocumentTemplate(
                        "Comprehensive Immigration Reform Act of 2025",
                        createImmigrationAct2025(),
                        "Federal Legislation",
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 8, 12),
                        "United States Congress",
                        "H.R. 2025-156",
                        List.of("citizenship pathway", "green card reform", "regional councils")
                ),

                // Airline Baggage - 2024
                new DocumentTemplate(
                        "Airline Consumer Protection Act of 2024",
                        createAirlineBaggageAct2024(),
                        "Federal Regulation",
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2023, 12, 28),
                        "Department of Transportation",
                        "DOT-2024-007",
                        List.of("free 50lb bag", "$200 delay compensation", "standardized carry-on")
                ),

                // Airline Baggage - 2025
                new DocumentTemplate(
                        "Enhanced Air Travel Standards Act of 2025",
                        createAirlineBaggageAct2025(),
                        "Federal Regulation",
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 8, 18),
                        "Federal Aviation Administration",
                        "FAA-2025-023",
                        List.of("70lb free bags", "smart luggage allowed", "real-time tracking")
                ),

                // National Park Naming - 2024
                new DocumentTemplate(
                        "National Park Heritage Preservation Act of 2024",
                        createParkNamingAct2024(),
                        "Federal Legislation",
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2023, 12, 30),
                        "National Park Service",
                        "NPS-2024-001",
                        List.of("naming committee", "2-year comment period", "Denali official name")
                ),

                // National Park Naming - 2025
                new DocumentTemplate(
                        "Indigenous Heritage Recognition Act of 2025",
                        createParkNamingAct2025(),
                        "Federal Legislation",
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 8, 25),
                        "National Park Service",
                        "NPS-2025-012",
                        List.of("tribal consultation required", "Indigenous Names Council", "50 park restoration program")
                )
        );
    }

    // Document content generation methods (same as before)
    private String createSpeedLimitAct2024() {
        return """
            HIGHWAY SPEED LIMIT MODERNIZATION ACT OF 2024
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "Highway Speed Limit Modernization Act of 2024".
            
            SECTION 2. FINDINGS
            Congress finds that:
            (1) Current speed limits were established in the 1970s during the energy crisis
            (2) Modern vehicles have significantly improved safety features
            (3) Highway infrastructure has been upgraded to handle higher speeds safely
            
            SECTION 3. SPEED LIMIT STANDARDS
            (a) RURAL INTERSTATE HIGHWAYS - The speed limit on rural interstate highways shall be 75 miles per hour, unless otherwise posted for safety reasons.
            
            (b) URBAN HIGHWAYS - The speed limit on highways within urban areas shall be 65 miles per hour, with local authorities able to reduce to no less than 55 mph in high-congestion areas.
            
            (c) RESIDENTIAL AREAS - The speed limit in residential areas shall be 25 miles per hour unless otherwise posted.
            
            SECTION 4. ENFORCEMENT
            States that do not comply with these standards within 18 months shall lose 10% of federal highway funding.
            
            SECTION 5. EFFECTIVE DATE
            This Act shall take effect on January 1, 2024.
            """;
    }

    private String createSpeedLimitAct2025() {
        return """
            AUTOMATED VEHICLE SPEED INTEGRATION ACT OF 2025
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "Automated Vehicle Speed Integration Act of 2025".
            
            SECTION 2. SUPERSESSION
            This Act supersedes the Highway Speed Limit Modernization Act of 2024 (H.R. 2024-001).
            
            SECTION 3. AUTOMATED VEHICLE PROVISIONS
            (a) CERTIFIED AUTONOMOUS VEHICLES - Vehicles certified as Level 4 or Level 5 autonomous may travel up to 85 miles per hour on rural interstate highways.
            
            (b) DYNAMIC SPEED ZONES - Highway authorities may establish dynamic speed limit zones that adjust based on:
            (1) Real-time traffic conditions
            (2) Weather conditions
            (3) Construction activities
            (4) Emergency situations
            
            SECTION 4. TRADITIONAL VEHICLE LIMITS
            Traditional manually-operated vehicles remain subject to the 75 mph limit established in the 2024 Act.
            
            SECTION 5. ENFORCEMENT TECHNOLOGY
            (a) AUTOMATED ENFORCEMENT - Automated speed enforcement systems may be deployed on highways with dynamic speed zones.
            
            (b) VEHICLE COMMUNICATION - Autonomous vehicles must communicate with highway infrastructure to receive real-time speed limit updates.
            
            SECTION 6. EFFECTIVE DATE
            This Act shall take effect on September 1, 2025.
            """;
    }

    private String createDrugPolicyAct2024() {
        return """
            CONTROLLED SUBSTANCE REFORM ACT OF 2024
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "Controlled Substance Reform Act of 2024".
            
            SECTION 2. CANNABIS RECLASSIFICATION
            (a) SCHEDULE CHANGE - Cannabis and cannabis-derived products are hereby moved from Schedule I to Schedule III of the Controlled Substances Act.
            
            (b) MEDICAL USE AUTHORIZATION - Licensed medical practitioners may prescribe cannabis for medical conditions in all states and territories.
            
            SECTION 3. DISTRIBUTION REQUIREMENTS
            (a) FEDERAL LICENSE REQUIRED - Distribution of cannabis products requires federal licensing through the Drug Enforcement Administration.
            
            (b) STATE COORDINATION - Federal licenses must be coordinated with state cannabis control authorities where they exist.
            
            SECTION 4. CRIMINAL PENALTIES
            (a) UNLICENSED DISTRIBUTION - Distribution without proper federal and state licensing remains a federal crime punishable by up to 5 years imprisonment.
            
            (b) POSSESSION LIMITS - Personal possession of up to 1 ounce for medical use is permitted with valid prescription.
            
            SECTION 5. RESEARCH PROVISIONS
            (a) EXPANDED RESEARCH - Universities and research institutions may conduct cannabis research with DEA approval.
            
            (b) CLINICAL TRIALS - FDA may approve clinical trials for cannabis-based medications.
            
            SECTION 6. EFFECTIVE DATE
            This Act shall take effect on January 1, 2024.
            """;
    }

    private String createDrugPolicyAct2025() {
        return """
            CANNABIS LEGALIZATION AND REGULATION ACT OF 2025
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "Cannabis Legalization and Regulation Act of 2025".
            
            SECTION 2. CONTROLLED SUBSTANCES ACT AMENDMENT
            Cannabis is hereby removed entirely from all schedules of the Controlled Substances Act.
            
            SECTION 3. FEDERAL TAXATION FRAMEWORK
            (a) EXCISE TAX - A federal excise tax of 10% shall be imposed on retail cannabis sales.
            
            (b) BUSINESS TAX DEDUCTIONS - Cannabis businesses may claim standard business tax deductions under Section 280E of the Internal Revenue Code.
            
            SECTION 4. INTERSTATE COMMERCE
            (a) COMMERCE CLAUSE - Cannabis may be transported across state lines where legal in both states.
            
            (b) BANKING SERVICES - Financial institutions may provide banking services to licensed cannabis businesses without federal penalty.
            
            SECTION 5. CRIMINAL JUSTICE REFORM
            (a) EXPUNGEMENT PROGRAM - The Attorney General shall establish a program to expunge federal cannabis convictions for non-violent offenses.
            
            (b) RELEASE PROGRAM - Individuals currently incarcerated solely for non-violent cannabis offenses shall be eligible for immediate release.
            
            SECTION 6. STATE AUTHORITY
            Nothing in this Act requires states to legalize cannabis or prevents states from maintaining prohibition.
            
            SECTION 7. EFFECTIVE DATE
            This Act shall take effect on September 1, 2025.
            """;
    }

    // Additional document content methods...
    private String createImmigrationAct2024() {
        return """
            BORDER SECURITY ENHANCEMENT ACT OF 2024
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "Border Security Enhancement Act of 2024".
            
            SECTION 2. BIOMETRIC VERIFICATION SYSTEM
            (a) MANDATORY IMPLEMENTATION - All ports of entry shall implement biometric verification systems within 12 months.
            
            (b) BIOMETRIC DATA COLLECTION - Fingerprints, facial recognition, and iris scans shall be collected from all non-citizens entering the United States.
            
            SECTION 3. VISITOR VISA EXTENSIONS
            (a) EXTENDED VALIDITY - B-1/B-2 visitor visas for nationals of allied countries shall be valid for 2 years instead of the current standard.
            
            (b) ELIGIBLE COUNTRIES - Countries with visa overstay rates below 2% are eligible for extended validity periods.
            
            SECTION 4. H-1B VISA PROGRAM EXPANSION
            (a) CAP INCREASE - The annual H-1B visa cap is increased by 15%, from 65,000 to 74,750 visas.
            
            (b) ADVANCED DEGREE EXEMPTION - The additional 20,000 visas for advanced degree holders remains unchanged.
            
            SECTION 5. EFFECTIVE DATE
            This Act shall take effect on January 1, 2024.
            """;
    }

    private String createImmigrationAct2025() {
        return """
            COMPREHENSIVE IMMIGRATION REFORM ACT OF 2025
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "Comprehensive Immigration Reform Act of 2025".
            
            SECTION 2. PATHWAY TO CITIZENSHIP
            (a) ELIGIBILITY - Undocumented immigrants present in the US for 8+ years may apply for legal status.
            
            (b) REQUIREMENTS - Applicants must pass background checks, pay back taxes, and demonstrate English proficiency.
            
            (c) TIMELINE - Legal permanent residency available after 5 years, citizenship after additional 3 years.
            
            SECTION 3. GREEN CARD REFORMS
            (a) PER-COUNTRY LIMITS ELIMINATED - The 7% per-country limit for employment-based green cards is abolished.
            
            (b) FAMILY REUNIFICATION - Processing times for family-based immigration reduced through increased annual limits.
            
            SECTION 4. REGIONAL IMMIGRATION COUNCILS
            (a) ESTABLISHMENT - Regional councils with local business and community representation shall advise on immigration needs.
            
            (b) AUTHORITY - Councils may recommend regional visa allocations based on economic conditions.
            
            SECTION 5. EFFECTIVE DATE
            This Act shall take effect on September 1, 2025.
            """;
    }

    private String createAirlineBaggageAct2024() {
        return """
            AIRLINE CONSUMER PROTECTION ACT OF 2024
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "Airline Consumer Protection Act of 2024".
            
            SECTION 2. CHECKED BAGGAGE REQUIREMENTS
            (a) FREE CHECKED BAG - Airlines must allow one free checked bag up to 50 pounds on domestic flights exceeding 2 hours.
            
            (b) WEIGHT LIMIT - Additional fees may only be charged for bags exceeding 50 pounds.
            
            SECTION 3. DELAYED BAGGAGE COMPENSATION
            (a) MANDATORY COMPENSATION - Airlines must pay $200 per day for baggage delayed more than 24 hours.
            
            (b) MAXIMUM LIABILITY - Total compensation capped at $1,500 per bag.
            
            SECTION 4. CARRY-ON STANDARDIZATION
            (a) STANDARD DIMENSIONS - All airlines must accept carry-on bags measuring 22" × 14" × 9".
            
            (b) WEIGHT LIMITS - Carry-on weight limits may not exceed 40 pounds.
            
            SECTION 5. EFFECTIVE DATE
            This Act shall take effect on January 1, 2024.
            """;
    }

    private String createAirlineBaggageAct2025() {
        return """
            ENHANCED AIR TRAVEL STANDARDS ACT OF 2025
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "Enhanced Air Travel Standards Act of 2025".
            
            SECTION 2. EXPANDED BAGGAGE ALLOWANCES
            (a) INCREASED WEIGHT LIMIT - Free checked baggage allowance increased to 70 pounds.
            
            (b) SUPERSESSION - This provision supersedes the 50-pound limit in the 2024 Consumer Protection Act.
            
            SECTION 3. SMART LUGGAGE PROVISIONS
            (a) REMOVABLE BATTERY REQUIREMENT - Smart luggage with removable lithium batteries allowed as carry-on.
            
            (b) TSA COORDINATION - TSA shall develop screening procedures for smart luggage within 90 days.
            
            SECTION 4. BAGGAGE TRACKING SYSTEMS
            (a) REAL-TIME TRACKING - All airports must implement real-time baggage tracking systems.
            
            (b) PASSENGER ACCESS - Passengers must have mobile access to baggage location data.
            
            SECTION 5. EFFECTIVE DATE
            This Act shall take effect on September 1, 2025.
            """;
    }

    private String createParkNamingAct2024() {
        return """
            NATIONAL PARK HERITAGE PRESERVATION ACT OF 2024
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "National Park Heritage Preservation Act of 2024".
            
            SECTION 2. NAMING REVIEW COMMITTEE
            (a) ESTABLISHMENT - A National Park Naming Review Committee is hereby established.
            
            (b) COMPOSITION - Committee consists of historians, tribal representatives, and park service officials.
            
            SECTION 3. NAME CHANGE PROCEDURES
            (a) PUBLIC COMMENT - All proposed name changes require 2-year public comment period.
            
            (b) HISTORICAL SIGNIFICANCE - Names with significant historical importance require super-majority committee approval.
            
            SECTION 4. DENALI NATIONAL PARK
            (a) OFFICIAL DESIGNATION - Mount McKinley National Park is hereby officially renamed Denali National Park.
            
            (b) HISTORICAL RECOGNITION - This change recognizes the original Koyukon name for the mountain.
            
            SECTION 5. EFFECTIVE DATE
            This Act shall take effect on January 1, 2024.
            """;
    }

    private String createParkNamingAct2025() {
        return """
            INDIGENOUS HERITAGE RECOGNITION ACT OF 2025
            
            SECTION 1. SHORT TITLE
            This Act may be cited as the "Indigenous Heritage Recognition Act of 2025".
            
            SECTION 2. TRIBAL CONSULTATION REQUIREMENTS
            (a) MANDATORY CONSULTATION - All park naming decisions must include consultation with affected tribal nations.
            
            (b) CONSULTATION TIMELINE - Tribal nations have 6 months to provide input on proposed changes.
            
            SECTION 3. INDIGENOUS NAMES COUNCIL
            (a) ESTABLISHMENT - An Indigenous Names Council with representatives from major tribal confederations.
            
            (b) VETO AUTHORITY - Council may veto naming decisions that conflict with indigenous heritage.
            
            SECTION 4. RESTORATION PROGRAM
            (a) 50-PARK INITIATIVE - Program to restore original indigenous names to 50 national parks by 2030.
            
            (b) FUNDING - $10 million annual appropriation for research and implementation.
            
            SECTION 5. SUPERSESSION
            This Act supersides conflicting provisions of the National Park Heritage Preservation Act of 2024.
            
            SECTION 6. EFFECTIVE DATE
            This Act shall take effect on September 1, 2025.
            """;
    }
}