package com.radiofind.track.schedule;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ScheduleChangeRequestBody(
    @NotNull @Future Instant preferredAt,
    @Size(max = 500) String comment) {
}
