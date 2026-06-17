package net.sixik.ga_utils.javatogpu.frontend.validation;

import java.util.List;

public final class GpuValidationException extends RuntimeException {

    private final List<GpuValidationIssue> issues;

    public GpuValidationException(List<GpuValidationIssue> issues) {
        super(issues.isEmpty() ? "GPU validation failed" : issues.get(0).message());
        this.issues = List.copyOf(issues);
    }

    public List<GpuValidationIssue> issues() {
        return issues;
    }
}
