package org.tanzu.ipzs.legislation.model.dto;

import java.util.List;

public record ChatResponseDto(
        String message,
        String sessionId,
        List<String> sources
) {}