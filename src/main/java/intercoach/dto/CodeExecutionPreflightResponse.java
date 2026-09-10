package intercoach.dto;

import java.time.Instant;

public record CodeExecutionPreflightResponse(
        boolean osIsolationRequired,
        boolean checked,
        boolean hostReady,
        boolean dockerAvailable,
        boolean imageReady,
        boolean imagePrePullEnabled,
        String message,
        Instant checkedAt
) {
}
