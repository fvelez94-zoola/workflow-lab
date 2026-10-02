package com.hexagonal.workflowlab.domain.usecase.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowLookupTest {

    @Mock
    private WorkflowRepository workflows;

    @Test
    void shouldReturnWorkflowWhenItExists() {
        Workflow workflow = Workflow.draft("Assay", null, List.of(), List.of());
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));

        assertThat(WorkflowLookup.requireById(workflows, workflow.getId())).isSameAs(workflow);
    }

    @Test
    void shouldThrowNotFoundWhenWorkflowDoesNotExist() {
        WorkflowId id = WorkflowId.generate();
        when(workflows.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> WorkflowLookup.requireById(workflows, id))
                .isInstanceOf(WorkflowNotFoundException.class);
    }
}
