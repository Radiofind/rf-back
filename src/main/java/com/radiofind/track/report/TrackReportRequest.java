package com.radiofind.track.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrackReportRequest(
    @NotNull TrackReportReason reason,
    @Size(max = 500) String comment) {
}
