package group16.webapp.common.dto;

import group16.webapp.common.exception.ConflictCode;

/** One violated rule on one schedule session (design doc "ConflictErrorDTO"). */
public record ConflictErrorDto(Long sessionId, ConflictCode code, String message) {
}
