package com.company.featureflag.scheduling.api;

import com.company.featureflag.common.logging.RequestContext;
import com.company.featureflag.scheduling.api.dto.CreateScheduleRequest;
import com.company.featureflag.scheduling.api.dto.ScheduleResponse;
import com.company.featureflag.scheduling.application.ScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/features/{featureKey}/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final RequestContext requestContext;

    public ScheduleController(ScheduleService scheduleService, RequestContext requestContext) {
        this.scheduleService = scheduleService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(@PathVariable String featureKey,
                                                     @Valid @RequestBody CreateScheduleRequest request,
                                                     HttpServletRequest httpRequest) {
        ScheduleResponse response = scheduleService.create(featureKey, request, requestContext.currentActor(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ScheduleResponse> list(@PathVariable String featureKey) {
        return scheduleService.list(featureKey);
    }
}
