package stirling.software.common.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * No-op PostHog configuration for Y-Edit desktop mode. Analytics are disabled.
 */
@Configuration
public class PostHogConfig {
    // No beans needed — PostHogService is a no-op stub
}
