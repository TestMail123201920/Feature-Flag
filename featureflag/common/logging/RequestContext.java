package com.company.featureflag.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Per spec §36, authentication/authorization is out of scope for now and will
 * be handled externally (e.g. by a gateway/BFF). Until then, callers identify
 * themselves via the X-User-Id header so createdBy/updatedBy fields are still
 * meaningful; this is the one seam that will need to change (to read from a
 * validated principal) once auth lands — everything else is unaffected.
 */
@Component
public class RequestContext {

    public static final String USER_ID_HEADER = "X-User-Id";
    private static final String DEFAULT_ACTOR = "system";

    public String currentActor(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        return (userId == null || userId.isBlank()) ? DEFAULT_ACTOR : userId;
    }
}
