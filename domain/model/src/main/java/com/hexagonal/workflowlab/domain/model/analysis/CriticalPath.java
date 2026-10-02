package com.hexagonal.workflowlab.domain.model.analysis;

import java.time.Duration;
import java.util.List;

/** The longest chain of dependent nodes: it decides how long the whole workflow takes. */
public record CriticalPath(Duration totalDuration, List<CriticalPathStep> steps) {

    public CriticalPath {
        steps = List.copyOf(steps);
    }
}
