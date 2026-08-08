package com.company.featureflag.evaluation.api;

import com.company.featureflag.evaluation.api.dto.BatchEvaluateRequest;
import com.company.featureflag.evaluation.api.dto.BatchEvaluateResponse;
import com.company.featureflag.evaluation.api.dto.EvaluateRequest;
import com.company.featureflag.evaluation.api.dto.EvaluateResponse;
import com.company.featureflag.evaluation.application.EvaluationEngine;
import com.company.featureflag.evaluation.domain.EvaluationContext;
import com.company.featureflag.evaluation.domain.EvaluationResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Direct HTTP evaluation for clients that cannot embed the Java SDK. For
 * high-throughput services, SDK + local evaluation (Phase 12-13) is the
 * preferred integration model — this endpoint pays a network round trip
 * per call.
 */
@RestController
@RequestMapping("/api/v1/evaluations")
public class EvaluationController {

    private final EvaluationEngine evaluationEngine;

    public EvaluationController(EvaluationEngine evaluationEngine) {
        this.evaluationEngine = evaluationEngine;
    }

    @PostMapping
    public EvaluateResponse evaluate(@Valid @RequestBody EvaluateRequest request) {
        EvaluationContext context = toContext(request.context());
        EvaluationResult result = evaluationEngine.evaluate(request.featureKey(), context);
        return toResponse(result);
    }

    @PostMapping("/batch")
    public BatchEvaluateResponse evaluateBatch(@Valid @RequestBody BatchEvaluateRequest request) {
        EvaluationContext context = toContext(request.context());
        List<EvaluateResponse> results = request.features().stream()
                .map(featureKey -> toResponse(evaluationEngine.evaluate(featureKey, context)))
                .toList();
        return new BatchEvaluateResponse(results);
    }

    private EvaluationContext toContext(Map<String, Object> fields) {
        return fields == null ? EvaluationContext.empty() : EvaluationContext.builder().putAll(fields).build();
    }

    private EvaluateResponse toResponse(EvaluationResult result) {
        return new EvaluateResponse(result.featureKey(), result.enabled(), result.version(), result.reason());
    }
}
