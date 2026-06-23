package net.sixik.ga_utils.javatogpu.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed backend API version used for capability matching.
 */
public record GpuRuntimeApiVersion(int major, int minor) implements Comparable<GpuRuntimeApiVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)");

    /**
     * Parses the first {@code major.minor} pair found in the given text.
     *
     * @param text raw version text such as {@code OpenCL 3.0 CUDA}
     * @return parsed version or {@code null} when no version pair is present
     */
    public static GpuRuntimeApiVersion parseFirst(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return new GpuRuntimeApiVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
        );
    }

    @Override
    public int compareTo(GpuRuntimeApiVersion other) {
        int majorCompare = Integer.compare(major, other.major);
        if (majorCompare != 0) {
            return majorCompare;
        }
        return Integer.compare(minor, other.minor);
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
