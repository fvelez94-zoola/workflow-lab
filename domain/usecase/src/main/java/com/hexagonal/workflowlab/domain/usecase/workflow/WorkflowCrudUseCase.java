package com.hexagonal.workflowlab.domain.usecase.workflow;

import com.hexagonal.workflowlab.domain.model.port.in.WorkflowCrud;
import com.hexagonal.workflowlab.domain.model.port.in.WorkflowDraft;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowCrudUseCase implements WorkflowCrud {

    private final WorkflowRepository workflows;

    @Override
    public Workflow create(WorkflowDraft draft) {
        return workflows.save(Workflow.draft(draft.name(), draft.description(), draft.nodes(), draft.dependencies()));
    }

    @Override
    public Workflow get(WorkflowId id) {
        return WorkflowLookup.requireById(workflows, id);
    }

    @Override
    public List<Workflow> list() {
        return workflows.findAll();
    }

    @Override
    public Workflow update(WorkflowId id, WorkflowDraft draft) {
        Workflow workflow = get(id);
        workflow.redefine(draft.name(), draft.description(), draft.nodes(), draft.dependencies());
        return workflows.save(workflow);
    }

    @Override
    public void delete(WorkflowId id) {
        get(id).requireDeletable();
        workflows.deleteById(id);
    }
}
