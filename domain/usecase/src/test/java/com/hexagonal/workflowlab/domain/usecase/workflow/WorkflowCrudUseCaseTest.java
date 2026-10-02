package com.hexagonal.workflowlab.domain.usecase.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.domain.model.port.in.WorkflowDraft;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowState;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Use case tests: the output port is mocked; the real domain objects are used. */
@ExtendWith(MockitoExtension.class)
class WorkflowCrudUseCaseTest {

    @Mock
    private WorkflowRepository workflows;

    private WorkflowCrudUseCase service;

    @BeforeEach
    void setUp() {
        service = new WorkflowCrudUseCase(workflows);
    }

    private static WorkflowDraft draft() {
        return new WorkflowDraft("Assay", "desc",
                List.of(new MixNode(new NodeId("a"), "Mix", 300, 30),
                        new MeasureNode(new NodeId("b"), "Read", MeasurementType.ABSORBANCE)),
                List.of(new Dependency(new NodeId("a"), new NodeId("b"))));
    }

    private static Workflow existing() {
        WorkflowDraft draft = draft();
        return Workflow.draft(draft.name(), draft.description(), draft.nodes(), draft.dependencies());
    }

    @Test
    void shouldSaveNewWorkflowAsDraft() {
        when(workflows.save(any(Workflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Workflow created = service.create(draft());

        assertThat(created.getState()).isEqualTo(WorkflowState.DRAFT);
        verify(workflows).save(created);
    }

    @Test
    void shouldReturnExistingWorkflow() {
        Workflow workflow = existing();
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));

        assertThat(service.get(workflow.getId())).isSameAs(workflow);
    }

    @Test
    void shouldThrowNotFoundWhenWorkflowIsMissing() {
        WorkflowId id = WorkflowId.generate();
        when(workflows.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(WorkflowNotFoundException.class);
    }

    @Test
    void shouldListAllWorkflows() {
        Workflow workflow = existing();
        when(workflows.findAll()).thenReturn(List.of(workflow));

        assertThat(service.list()).containsExactly(workflow);
    }

    @Test
    void shouldRedefineAndSaveDraft() {
        Workflow workflow = existing();
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));
        when(workflows.save(workflow)).thenReturn(workflow);

        Workflow updated = service.update(workflow.getId(),
                new WorkflowDraft("Renamed", null, List.of(), List.of()));

        assertThat(updated.getName()).isEqualTo("Renamed");
        verify(workflows).save(workflow);
    }

    @Test
    void shouldNotSaveWhenUpdatingPublishedWorkflow() {
        Workflow workflow = existing();
        workflow.publish();
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> service.update(workflow.getId(), draft()))
                .isInstanceOf(InvalidWorkflowStateException.class);
        verify(workflows, never()).save(any());
    }

    @Test
    void shouldDeleteDraftWorkflow() {
        Workflow workflow = existing();
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));

        service.delete(workflow.getId());

        verify(workflows).deleteById(workflow.getId());
    }

    @Test
    void shouldNotDeletePublishedWorkflow() {
        Workflow workflow = existing();
        workflow.publish();
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));

        assertThatThrownBy(() -> service.delete(workflow.getId())).isInstanceOf(InvalidWorkflowStateException.class);
        verify(workflows, never()).deleteById(any());
    }
}
