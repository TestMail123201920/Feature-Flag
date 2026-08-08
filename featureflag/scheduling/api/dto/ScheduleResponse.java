package com.company.featureflag.scheduling.api.dto;

import com.company.featureflag.scheduling.domain.ScheduledAction;
import com.company.featureflag.scheduling.domain.ScheduledJobStatus;

import java.time.Instant;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        ScheduledAction action,
        Instant scheduledTime,
        ScheduledJobStatus status,
        Instant executedAt
) {
}
