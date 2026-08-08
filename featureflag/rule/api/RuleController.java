package com.company.featureflag.rule.api;

import com.company.featureflag.common.logging.RequestContext;
import com.company.featureflag.rule.api.dto.CreateRuleRequest;
import com.company.featureflag.rule.api.dto.RuleResponse;
import com.company.featureflag.rule.api.dto.UpdateRuleRequest;
import com.company.featureflag.rule.application.RuleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/features/{featureKey}/rules")
public class RuleController {

    private final RuleService ruleService;
    private final RequestContext requestContext;

    public RuleController(RuleService ruleService, RequestContext requestContext) {
        this.ruleService = ruleService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public ResponseEntity<RuleResponse> addRule(@PathVariable String featureKey,
                                                 @Valid @RequestBody CreateRuleRequest request,
                                                 HttpServletRequest httpRequest) {
        RuleResponse response = ruleService.addRule(featureKey, request, requestContext.currentActor(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{ruleId}")
    public RuleResponse updateRule(@PathVariable String featureKey,
                                    @PathVariable UUID ruleId,
                                    @Valid @RequestBody UpdateRuleRequest request,
                                    HttpServletRequest httpRequest) {
        return ruleService.updateRule(featureKey, ruleId, request, requestContext.currentActor(httpRequest));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable String featureKey,
                                            @PathVariable UUID ruleId,
                                            HttpServletRequest httpRequest) {
        ruleService.deleteRule(featureKey, ruleId, requestContext.currentActor(httpRequest));
        return ResponseEntity.noContent().build();
    }
}
