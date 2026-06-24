package net.sixik.ga_utils.javatogpu.benchmark;

public record BenchmarkResult(
        String name,
        int warmupIterations,
        int measuredIterations,
        long totalNanos,
        long minNanos,
        long maxNanos,
        long averageNanos,
        long checksum
) {
    public String format() {
        return "[benchmark] "
                + name
                + " warmup="
                + warmupIterations
                + " measure="
                + measuredIterations
                + " avg="
                + averageNanos
                + "ns min="
                + minNanos
                + "ns max="
                + maxNanos
                + "ns checksum="
                + checksum;
    }
}
