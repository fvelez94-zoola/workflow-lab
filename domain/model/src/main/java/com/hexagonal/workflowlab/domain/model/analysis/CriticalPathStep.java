package com.hexagonal.workflowlab.domain.model.analysis;

import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import java.time.Duration;

public record CriticalPathStep(NodeId nodeId, String nodeName, Duration duration) {
}
