package com.company.featureflag.feature.api;

import com.company.featureflag.common.dto.PageResponse;
import com.company.featureflag.common.logging.RequestContext;
import com.company.featureflag.feature.api.dto.*;
import com.company.featureflag.feature.application.FeatureService;
import com.company.featureflag.feature.application.FeatureVersionService;
import com.company.featureflag.feature.domain.FeatureStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/features")
public class FeatureController {

    private final FeatureService featureService;
    private final FeatureVersionService featureVersionService;
    private final RequestContext requestContext;

    public FeatureController(FeatureService featureService,
                              FeatureVersionService featureVersionService,
                              RequestContext requestContext) {
        this.featureService = featureService;
        this.featureVersionService = featureVersionService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public ResponseEntity<FeatureResponse> create(@Valid @RequestBody CreateFeatureRequest request,
                                                   HttpServletRequest httpRequest) {
        FeatureResponse response = featureService.create(request, requestContext.currentActor(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public PageResponse<FeatureResponse> list(@RequestParam(required = false) FeatureStatus status,
                                               Pageable pageable) {
        return PageResponse.from(featureService.list(status, pageable));
    }

    @GetMapping("/{featureKey}")
    public FeatureResponse getByKey(@PathVariable String featureKey) {
        return featureService.getByKey(featureKey);
    }

    @PutMapping("/{featureKey}")
    public FeatureResponse update(@PathVariable String featureKey,
                                   @Valid @RequestBody UpdateFeatureRequest request,
                                   HttpServletRequest httpRequest) {
        return featureService.update(featureKey, request, requestContext.currentActor(httpRequest));
    }

    @DeleteMapping("/{featureKey}")
    public ResponseEntity<Void> delete(@PathVariable String featureKey, HttpServletRequest httpRequest) {
        featureService.delete(featureKey, requestContext.currentActor(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{featureKey}/versions")
    public java.util.List<FeatureVersionSummaryResponse> listVersions(@PathVariable String featureKey) {
        return featureVersionService.listVersions(featureKey);
    }

    @GetMapping("/{featureKey}/versions/{version}")
    public FeatureVersionDetailResponse getVersion(@PathVariable String featureKey, @PathVariable int version) {
        return featureVersionService.getVersion(featureKey, version);
    }

    @PostMapping("/{featureKey}/rollback")
    public FeatureVersionDetailResponse rollback(@PathVariable String featureKey,
                                                  @Valid @RequestBody RollbackRequest request,
                                                  HttpServletRequest httpRequest) {
        return featureVersionService.rollback(featureKey, request.targetVersionNumber(),
                requestContext.currentActor(httpRequest));
    }
}
