package com.company.featureflag.scheduling.api.dto;

import com.company.featureflag.scheduling.domain.ScheduledAction;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateScheduleRequest(
        @NotNull ScheduledAction action,
        @NotNull @Future Instant scheduledTime,
        Integer targetVersionNumber
) {
}
