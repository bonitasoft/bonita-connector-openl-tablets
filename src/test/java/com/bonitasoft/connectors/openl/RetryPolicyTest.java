package com.bonitasoft.connectors.openl;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void shouldReturnResultOnFirstSuccess() throws Exception {
        RetryPolicy policy = new RetryPolicy(3);

        String result = policy.execute(() -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void shouldRetryOnRetryableException() throws Exception {
        RetryPolicy policy = new NoSleepRetryPolicy(3);
        var counter = new int[]{0};

        String result = policy.execute(() -> {
            counter[0]++;
            if (counter[0] < 3) {
                throw new OpenLException("Rate limited", 429, true);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(counter[0]).isEqualTo(3);
    }

    @Test
    void shouldFailImmediatelyOnNonRetryableException() {
        RetryPolicy policy = new NoSleepRetryPolicy(3);

        assertThatThrownBy(() -> policy.execute(() -> {
            throw new OpenLException("Unauthorized", 401, false);
        }))
                .isInstanceOf(OpenLException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void shouldFailAfterMaxRetries() {
        RetryPolicy policy = new NoSleepRetryPolicy(2);

        assertThatThrownBy(() -> policy.execute(() -> {
            throw new OpenLException("Server error", 500, true);
        }))
                .isInstanceOf(OpenLException.class)
                .hasMessageContaining("Server error");
    }

    @Test
    void shouldIdentifyRetryableStatusCodes() {
        assertThat(RetryPolicy.isRetryableStatusCode(429)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(500)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(502)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(503)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(400)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(401)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(404)).isFalse();
    }

    @Test
    void shouldCalculateExponentialBackoff() {
        RetryPolicy policy = new RetryPolicy(5);

        long wait0 = policy.calculateWait(0);
        long wait1 = policy.calculateWait(1);
        long wait2 = policy.calculateWait(2);

        // Exponential growth with jitter: base * 2^attempt + jitter
        assertThat(wait0).isBetween(1000L, 1500L);
        assertThat(wait1).isBetween(2000L, 3000L);
        assertThat(wait2).isBetween(4000L, 6000L);
    }

    /** RetryPolicy subclass that skips sleep for testing. */
    private static class NoSleepRetryPolicy extends RetryPolicy {
        NoSleepRetryPolicy(int maxRetries) {
            super(maxRetries);
        }

        @Override
        void sleep(long millis) {
            // No-op for testing
        }
    }
}
