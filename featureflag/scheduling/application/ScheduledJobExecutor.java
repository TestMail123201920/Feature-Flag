package com.company.featureflag.scheduling.application;

import com.company.featureflag.feature.application.FeatureService;
import com.company.featureflag.feature.application.FeatureVersionService;
import com.company.featureflag.feature.domain.FeatureStatus;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import com.company.featureflag.scheduling.domain.ScheduledJob;
import com.company.featureflag.scheduling.infrastructure.ScheduledJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls due {@link ScheduledJob}s and applies them (spec §15). Plain
 * {@code @Scheduled} polling for now — the job shape (feature, action,
 * time, status, metadata) is generic enough to hand off to a distributed
 * scheduler later without a data model change.
 *
 * ENABLE/DISABLE are interpreted as the feature's ACTIVE/ARCHIVED lifecycle
 * (go-live / retire); kill switch is controlled independently via the
 * ACTIVATE_KILL_SWITCH/DEACTIVATE_KILL_SWITCH actions — the spec doesn't
 * spell out this distinction, so it's called out here explicitly.
 */
@Component
public class ScheduledJobExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobExecutor.class);
    private static final String ACTOR = "scheduler";

    private final ScheduledJobRepository scheduledJobRepository;
    private final FeatureRepository featureRepository;
    private final FeatureService featureService;
    private final FeatureVersionService featureVersionService;

    public ScheduledJobExecutor(ScheduledJobRepository scheduledJobRepository,
                                 FeatureRepository featureRepository,
                                 FeatureService featureService,
                                 FeatureVersionService featureVersionService) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.featureRepository = featureRepository;
        this.featureService = featureService;
        this.featureVersionService = featureVersionService;
    }

    @Scheduled(fixedDelayString = "${feature-flag.scheduler.poll-interval-ms:10000}")
    public void executeDueJobs() {
        List<ScheduledJob> due = scheduledJobRepository.findByStatusAndScheduledTimeLessThanEqual(
                com.company.featureflag.scheduling.domain.ScheduledJobStatus.PENDING, Instant.now());

        for (ScheduledJob job : due) {
            try {
                execute(job);
                job.markExecuted();
            } catch (Exception ex) {
                log.error("Failed to execute scheduled job {} (action={})", job.getId(), job.getAction(), ex);
                job.markFailed(ex.getMessage());
            }
            scheduledJobRepository.save(job);
        }
    }

    @Transactional
    void execute(ScheduledJob job) {
        String featureKey = featureRepository.findById(job.getFeatureId())
                .orElseThrow(() -> new IllegalStateException("Feature no longer exists for scheduled job " + job.getId()))
                .getKey();

        switch (job.getAction()) {
            case ENABLE -> featureService.transitionStatus(featureKey, FeatureStatus.ACTIVE, ACTOR);
            case DISABLE -> featureService.transitionStatus(featureKey, FeatureStatus.ARCHIVED, ACTOR);
            case ACTIVATE_KILL_SWITCH -> featureService.setKillSwitch(featureKey, true, ACTOR);
            case DEACTIVATE_KILL_SWITCH -> featureService.setKillSwitch(featureKey, false, ACTOR);
            case ACTIVATE_VERSION -> {
                Object targetVersion = job.getExecutionMetadata().get("targetVersionNumber");
                if (targetVersion == null) {
                    throw new IllegalStateException("ACTIVATE_VERSION job missing targetVersionNumber: " + job.getId());
                }
                featureVersionService.rollback(featureKey, ((Number) targetVersion).intValue(), ACTOR);
            }
        }
    }
}
