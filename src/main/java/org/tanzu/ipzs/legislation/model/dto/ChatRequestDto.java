package org.tanzu.ipzs.legislation.model.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ChatRequestDto(
        String message,
        LocalDate dateContext,
        UUID sessionId
) {}