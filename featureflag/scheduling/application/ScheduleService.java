package com.company.featureflag.scheduling.application;

import com.company.featureflag.common.error.FeatureNotFoundException;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import com.company.featureflag.scheduling.api.dto.CreateScheduleRequest;
import com.company.featureflag.scheduling.api.dto.ScheduleResponse;
import com.company.featureflag.scheduling.domain.ScheduledAction;
import com.company.featureflag.scheduling.domain.ScheduledJob;
import com.company.featureflag.scheduling.infrastructure.ScheduledJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ScheduleService {

    private final FeatureRepository featureRepository;
    private final ScheduledJobRepository scheduledJobRepository;

    public ScheduleService(FeatureRepository featureRepository, ScheduledJobRepository scheduledJobRepository) {
        this.featureRepository = featureRepository;
        this.scheduledJobRepository = scheduledJobRepository;
    }

    @Transactional
    public ScheduleResponse create(String featureKey, CreateScheduleRequest request, String actor) {
        Feature feature = findFeatureOrThrow(featureKey);

        if (request.action() == ScheduledAction.ACTIVATE_VERSION && request.targetVersionNumber() == null) {
            throw new IllegalArgumentException("targetVersionNumber is required for ACTIVATE_VERSION schedules");
        }

        Map<String, Object> metadata = request.targetVersionNumber() == null
                ? Map.of()
                : Map.of("targetVersionNumber", request.targetVersionNumber());

        ScheduledJob job = ScheduledJob.of(feature.getId(), request.action(), request.scheduledTime(), metadata);
        scheduledJobRepository.save(job);
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(String featureKey) {
        Feature feature = findFeatureOrThrow(featureKey);
        return scheduledJobRepository.findByFeatureId(feature.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private ScheduleResponse toResponse(ScheduledJob job) {
        return new ScheduleResponse(job.getId(), job.getAction(), job.getScheduledTime(), job.getStatus(), job.getExecutedAt());
    }

    private Feature findFeatureOrThrow(String key) {
        return featureRepository.findByKey(key)
                .orElseThrow(() -> new FeatureNotFoundException(key));
    }
}
