package com.company.featureflag.rollout.api;

import com.company.featureflag.common.logging.RequestContext;
import com.company.featureflag.rollout.api.dto.RolloutResponse;
import com.company.featureflag.rollout.api.dto.SetRolloutRequest;
import com.company.featureflag.rollout.application.RolloutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/features/{featureKey}/rollout")
public class RolloutController {

    private final RolloutService rolloutService;
    private final RequestContext requestContext;

    public RolloutController(RolloutService rolloutService, RequestContext requestContext) {
        this.rolloutService = rolloutService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public RolloutResponse create(@PathVariable String featureKey,
                                   @Valid @RequestBody SetRolloutRequest request,
                                   HttpServletRequest httpRequest) {
        return rolloutService.setRollout(featureKey, request, requestContext.currentActor(httpRequest));
    }

    @PutMapping
    public RolloutResponse replace(@PathVariable String featureKey,
                                    @Valid @RequestBody SetRolloutRequest request,
                                    HttpServletRequest httpRequest) {
        return rolloutService.setRollout(featureKey, request, requestContext.currentActor(httpRequest));
    }
}
