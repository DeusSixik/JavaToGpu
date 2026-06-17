package net.sixik.ga_utils.javatogpu.frontend.opencl;

import java.util.Set;

public final class OpenClKernelNaming {

    private static final Set<String> RESERVED_NAMES = Set.of(
            "kernel",
            "__kernel",
            "global",
            "__global",
            "local",
            "__local",
            "constant",
            "__constant",
            "private",
            "__private"
    );

    private OpenClKernelNaming() {
    }

    public static String toEntryPointName(String javaMethodName) {
        if (javaMethodName == null || javaMethodName.isBlank()) {
            return "jtg_kernel";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < javaMethodName.length(); i++) {
            char current = javaMethodName.charAt(i);
            boolean valid = i == 0
                    ? Character.isLetter(current) || current == '_'
                    : Character.isLetterOrDigit(current) || current == '_';
            builder.append(valid ? current : '_');
        }

        String candidate = builder.toString();
        if (candidate.isEmpty()) {
            candidate = "jtg_kernel";
        }
        if (!(Character.isLetter(candidate.charAt(0)) || candidate.charAt(0) == '_')) {
            candidate = "jtg_" + candidate;
        }
        if (candidate.startsWith("__") || RESERVED_NAMES.contains(candidate)) {
            candidate = "jtg_" + candidate;
        }

        return candidate;
    }
}
