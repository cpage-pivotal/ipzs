package org.tanzu.ipzs.legislation.model.dto;

import java.util.List;
import java.util.UUID;

public record ChatResponseDto(
        String message,
        UUID sessionId,
        List<String> sources
) {}