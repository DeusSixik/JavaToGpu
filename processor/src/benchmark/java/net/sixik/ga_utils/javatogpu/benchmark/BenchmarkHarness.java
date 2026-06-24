package net.sixik.ga_utils.javatogpu.benchmark;

import java.util.function.LongSupplier;

public final class BenchmarkHarness {

    private BenchmarkHarness() {
    }

    public static BenchmarkResult measure(String name, int warmupIterations, int measuredIterations, LongSupplier operation) {
        for (int iteration = 0; iteration < warmupIterations; iteration++) {
            operation.getAsLong();
        }

        long checksum = 0L;
        long totalNanos = 0L;
        long minNanos = Long.MAX_VALUE;
        long maxNanos = Long.MIN_VALUE;

        for (int iteration = 0; iteration < measuredIterations; iteration++) {
            long startedAt = System.nanoTime();
            checksum += operation.getAsLong();
            long duration = System.nanoTime() - startedAt;
            totalNanos += duration;
            minNanos = Math.min(minNanos, duration);
            maxNanos = Math.max(maxNanos, duration);
        }

        BenchmarkResult result = new BenchmarkResult(
                name,
                warmupIterations,
                measuredIterations,
                totalNanos,
                minNanos == Long.MAX_VALUE ? 0L : minNanos,
                maxNanos == Long.MIN_VALUE ? 0L : maxNanos,
                measuredIterations == 0 ? 0L : totalNanos / measuredIterations,
                checksum
        );
        System.out.println(result.format());
        return result;
    }
}
