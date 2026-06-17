package net.sixik.ga_utils.javatogpu.frontend.validation;

public record GpuValidationIssue(
        int line,
        int column,
        String message
) {
}
