package com.radiofind.track.review;

import java.time.Instant;

public record RejectionReasonResponse(String summary, String details, String reviewer, Instant reviewedAt) {
}
