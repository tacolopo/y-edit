package stirling.software.common.service;

import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * No-op PostHog analytics service for Y-Edit desktop mode. All methods are stubs that do nothing.
 */
@Service
public class PostHogService {

    public void captureEvent(String eventName, Map<String, Object> properties) {
        // No-op: analytics disabled in desktop mode
    }

    public void captureEvent(
            String eventName, Map<String, Object> properties, String distinctId) {
        // No-op
    }

    public void shutdown() {
        // No-op
    }
}
