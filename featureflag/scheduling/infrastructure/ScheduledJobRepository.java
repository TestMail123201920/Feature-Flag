package com.company.featureflag.scheduling.infrastructure;

import com.company.featureflag.scheduling.domain.ScheduledJob;
import com.company.featureflag.scheduling.domain.ScheduledJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, UUID> {
    List<ScheduledJob> findByFeatureId(UUID featureId);
    List<ScheduledJob> findByStatusAndScheduledTimeLessThanEqual(ScheduledJobStatus status, Instant now);
}
