package com.company.featureflag.feature.application;

import com.company.featureflag.common.error.DuplicateFeatureKeyException;
import com.company.featureflag.common.error.FeatureNotFoundException;
import com.company.featureflag.configuration.application.FeatureConfigurationInvalidator;
import com.company.featureflag.feature.api.dto.CreateFeatureRequest;
import com.company.featureflag.feature.api.dto.FeatureResponse;
import com.company.featureflag.feature.api.dto.UpdateFeatureRequest;
import com.company.featureflag.feature.domain.Feature;
import com.company.featureflag.feature.domain.FeatureStatus;
import com.company.featureflag.feature.domain.FeatureVersion;
import com.company.featureflag.feature.infrastructure.FeatureRepository;
import com.company.featureflag.outbox.domain.OutboxEvent;
import com.company.featureflag.outbox.infrastructure.OutboxEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Application service for feature lifecycle (create/read/update/archive).
 * Version creation on write is delegated to {@link FeatureVersionService} so
 * this class stays focused on the Feature aggregate itself.
 */
@Service
public class FeatureService {

    private final FeatureRepository featureRepository;
    private final FeatureVersionService featureVersionService;
    private final OutboxEventRepository outboxEventRepository;
    private final FeatureMapper featureMapper;
    private final FeatureConfigurationInvalidator cacheInvalidator;

    public FeatureService(FeatureRepository featureRepository,
                           FeatureVersionService featureVersionService,
                           OutboxEventRepository outboxEventRepository,
                           FeatureMapper featureMapper,
                           FeatureConfigurationInvalidator cacheInvalidator) {
        this.featureRepository = featureRepository;
        this.featureVersionService = featureVersionService;
        this.outboxEventRepository = outboxEventRepository;
        this.featureMapper = featureMapper;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Transactional
    public FeatureResponse create(CreateFeatureRequest request, String actor) {
        if (featureRepository.existsByKey(request.key())) {
            throw new DuplicateFeatureKeyException(request.key());
        }

        Feature feature = Feature.create(request.key(), request.name(), request.description(), actor);
        featureRepository.save(feature);

        // Every feature starts with an initial DRAFT version (v1, empty
        // configuration) so rules/rollout added later (Phase 6-7) always
        // have a version to attach to; it becomes the current version once
        // the feature is first activated or explicitly configured.
        FeatureVersion initialVersion = featureVersionService.createDraftVersion(
                feature.getId(), actor, Map.of("note", "initial version"));
        feature.pointToVersion(initialVersion.getVersionNumber(), actor);

        outboxEventRepository.save(OutboxEvent.of(
                "FEATURE_CREATED", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey(), "version", initialVersion.getVersionNumber())));

        return featureMapper.toResponse(feature);
    }

    @Transactional(readOnly = true)
    public FeatureResponse getByKey(String key) {
        return featureMapper.toResponse(findFeatureOrThrow(key));
    }

    @Transactional(readOnly = true)
    public Page<FeatureResponse> list(FeatureStatus statusFilter, Pageable pageable) {
        Page<Feature> page = statusFilter == null
                ? featureRepository.findAll(pageable)
                : featureRepository.findByStatus(statusFilter, pageable);
        return page.map(featureMapper::toResponse);
    }

    @Transactional
    public FeatureResponse update(String key, UpdateFeatureRequest request, String actor) {
        Feature feature = findFeatureOrThrow(key);

        feature.rename(request.name(), request.description(), actor);

        if (request.status() != null && request.status() != feature.getStatus()) {
            feature.transitionTo(request.status(), actor);
        }

        if (request.killSwitch() != null) {
            if (request.killSwitch()) {
                feature.activateKillSwitch(actor);
            } else {
                feature.deactivateKillSwitch(actor);
            }
        }

        outboxEventRepository.save(OutboxEvent.of(
                "FEATURE_UPDATED", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey(), "status", feature.getStatus().name(),
                        "killSwitch", feature.isKillSwitch())));
        cacheInvalidator.evictAfterCommit(feature.getKey());

        return featureMapper.toResponse(feature);
    }

    /**
     * Soft delete: transitions the feature to ARCHIVED rather than a hard
     * DELETE. A hard delete would cascade-remove feature_version history via
     * FK, which contradicts the spec's own emphasis on retaining version
     * history for audit/rollback — archived features are simply excluded
     * from active listings and evaluation (kill-switch semantics apply).
     */
    @Transactional
    public void delete(String key, String actor) {
        Feature feature = findFeatureOrThrow(key);
        feature.transitionTo(FeatureStatus.ARCHIVED, actor);

        outboxEventRepository.save(OutboxEvent.of(
                "FEATURE_ARCHIVED", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey())));
        cacheInvalidator.evictAfterCommit(feature.getKey());
    }

    Feature findFeatureOrThrow(String key) {
        return featureRepository.findByKey(key)
                .orElseThrow(() -> new FeatureNotFoundException(key));
    }

    /** Used directly by {@code update()} and by the scheduled-job executor (spec §15). */
    @Transactional
    public void transitionStatus(String key, FeatureStatus target, String actor) {
        Feature feature = findFeatureOrThrow(key);
        feature.transitionTo(target, actor);
        outboxEventRepository.save(OutboxEvent.of(
                "FEATURE_UPDATED", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey(), "status", feature.getStatus().name())));
        cacheInvalidator.evictAfterCommit(feature.getKey());
    }

    /** Used directly by {@code update()} and by the scheduled-job executor (spec §15). */
    @Transactional
    public void setKillSwitch(String key, boolean active, String actor) {
        Feature feature = findFeatureOrThrow(key);
        if (active) {
            feature.activateKillSwitch(actor);
        } else {
            feature.deactivateKillSwitch(actor);
        }
        outboxEventRepository.save(OutboxEvent.of(
                "FEATURE_UPDATED", "FEATURE", feature.getId(),
                Map.of("featureKey", feature.getKey(), "killSwitch", feature.isKillSwitch())));
        cacheInvalidator.evictAfterCommit(feature.getKey());
    }
}
