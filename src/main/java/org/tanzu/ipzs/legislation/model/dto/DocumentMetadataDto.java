package org.tanzu.ipzs.legislation.model.dto;

import java.time.LocalDate;

public record DocumentMetadataDto(
        String documentId,
        String title,
        String documentType,
        LocalDate publicationDate,
        LocalDate effectiveDate,
        LocalDate expirationDate,
        String issuingAuthority
) {}