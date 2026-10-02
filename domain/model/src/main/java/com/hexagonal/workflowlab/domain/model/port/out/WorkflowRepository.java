package com.hexagonal.workflowlab.domain.model.port.out;

import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import java.util.List;
import java.util.Optional;

/** Output port: how the domain stores workflows. The domain does not know (or care) with what. */
public interface WorkflowRepository {

    Workflow save(Workflow workflow);

    Optional<Workflow> findById(WorkflowId id);

    List<Workflow> findAll();

    void deleteById(WorkflowId id);
}
