package com.company.paymentservice;

import com.company.featureflag.sdk.core.EvaluationContext;
import com.company.featureflag.sdk.core.EvaluationResult;
import com.company.featureflag.sdk.core.FeatureFlagClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Demonstrates the whole point of the SDK (spec §59): this controller never
 * makes a network call to the Feature Flag Service on the request path.
 * {@code featureFlagClient} was auto-configured by
 * {@code FeatureFlagAutoConfiguration} and has been polling
 * {@code GET /api/v1/sdk/configuration} in the background since startup;
 * {@link FeatureFlagClient#evaluate} below reads only the in-memory snapshot.
 */
@RestController
public class PaymentController {

    private final FeatureFlagClient featureFlagClient;

    public PaymentController(FeatureFlagClient featureFlagClient) {
        this.featureFlagClient = featureFlagClient;
    }

    @GetMapping("/pay")
    public Map<String, Object> pay(@RequestParam String phoneNumber,
                                    @RequestParam(defaultValue = "IN") String country,
                                    @RequestParam(defaultValue = "STANDARD") String customerType) {

        EvaluationContext context = EvaluationContext.builder()
                .put("phoneNumber", phoneNumber)
                .put("country", country)
                .put("customerType", customerType)
                .put("requestId", UUID.randomUUID().toString())
                .build();

        EvaluationResult result = featureFlagClient.evaluate("NEW_PAYMENT_FLOW", context);

        String pathTaken;
        if (result.enabled()) {
            pathTaken = "NEW payment flow";
            // ... new implementation would run here
        } else {
            pathTaken = "OLD payment flow";
            // ... old, battle-tested implementation runs here
        }

        return Map.of(
                "pathTaken", pathTaken,
                "featureKey", result.featureKey(),
                "enabled", result.enabled(),
                "version", result.version() == null ? "n/a" : result.version(),
                "reason", result.reason());
    }
}
