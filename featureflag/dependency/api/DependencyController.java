package com.company.featureflag.dependency.api;

import com.company.featureflag.common.logging.RequestContext;
import com.company.featureflag.dependency.api.dto.CreateDependencyRequest;
import com.company.featureflag.dependency.api.dto.DependencyResponse;
import com.company.featureflag.dependency.application.DependencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/features/{featureKey}/dependencies")
public class DependencyController {

    private final DependencyService dependencyService;
    private final RequestContext requestContext;

    public DependencyController(DependencyService dependencyService, RequestContext requestContext) {
        this.dependencyService = dependencyService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public ResponseEntity<DependencyResponse> addDependency(@PathVariable String featureKey,
                                                              @Valid @RequestBody CreateDependencyRequest request,
                                                              HttpServletRequest httpRequest) {
        DependencyResponse response = dependencyService.addDependency(
                featureKey, request, requestContext.currentActor(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
