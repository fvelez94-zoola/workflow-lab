package com.hexagonal.workflowlab.domain.model.port.in;

import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowNode;
import java.util.List;

/** What an author provides to create or redefine a workflow. */
public record WorkflowDraft(String name, String description, List<WorkflowNode> nodes,
        List<Dependency> dependencies) {
}
