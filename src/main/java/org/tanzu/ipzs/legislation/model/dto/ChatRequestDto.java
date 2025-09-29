package org.tanzu.ipzs.legislation.model.dto;

import java.time.LocalDate;

public record ChatRequestDto(
        String message,
        LocalDate dateContext,
        String sessionId
) {}