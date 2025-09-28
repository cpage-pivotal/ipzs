package org.tanzu.ipzs.legislation.util;

import org.springframework.ai.document.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LegislationDateUtils {

    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Checks if a document is effective on the given query date.
     * A document is effective if its effective date is on or before the query date
     * and it has not expired (if expiration date exists).
     */
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
            // If date parsing fails, include the document
            return true;
        }
    }

    /**
     * Builds a filter expression for vector store queries.
     * Uses epoch days for numeric comparison in the vector store.
     */
    public static String buildDateFilterExpression(LocalDate contextDate) {
        if (contextDate == null) {
            return null;
        }

        long epochDays = contextDate.toEpochDay();
        return "effective_date_epoch <= " + epochDays;
    }

    /**
     * Formats a LocalDate for display in prompts and user messages.
     */
    public static String formatDateForDisplay(LocalDate date) {
        if (date == null) {
            return "current date";
        }
        return date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    /**
     * Parses effective date epoch from document metadata.
     */
    public static Long getEffectiveDateEpoch(Document document) {
        Object epochValue = document.getMetadata().get("effective_date_epoch");
        if (epochValue instanceof Number number) {
            return number.longValue();
        }

        // Fallback to parsing from ISO date string
        String effectiveDateStr = (String) document.getMetadata().get("effective_date");
        if (effectiveDateStr != null) {
            try {
                LocalDate effectiveDate = LocalDate.parse(effectiveDateStr, ISO_DATE_FORMATTER);
                return effectiveDate.toEpochDay();
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        return null;
    }
}