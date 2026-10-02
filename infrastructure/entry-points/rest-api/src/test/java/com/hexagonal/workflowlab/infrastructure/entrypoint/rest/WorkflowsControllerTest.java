package com.hexagonal.workflowlab.infrastructure.entrypoint.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.exception.ValidationFailure;
import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.domain.model.port.in.PublishWorkflow;
import com.hexagonal.workflowlab.domain.model.port.in.WorkflowCrud;
import com.hexagonal.workflowlab.domain.model.port.in.WorkflowDraft;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.error.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Entry-point test: only HTTP concerns are checked. The input ports are mocked, so no use case, no
 * database and no Spring context are involved.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowsControllerTest {

    private static final String VALID_REQUEST = """
            {
              "name": "Assay",
              "nodes": [
                {"id": "mix-1", "name": "Mix", "type": "MIX", "speedRpm": 300, "durationSeconds": 30},
                {"id": "read-1", "name": "Read", "type": "MEASURE", "measurementType": "ABSORBANCE"}
              ],
              "dependencies": [{"from": "mix-1", "to": "read-1"}]
            }
            """;

    @Mock
    private WorkflowCrud workflowCrud;
    @Mock
    private PublishWorkflow publishWorkflow;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new WorkflowsController(workflowCrud, publishWorkflow))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static Workflow sampleWorkflow() {
        return Workflow.draft("Assay", null,
                List.of(new MixNode(new NodeId("mix-1"), "Mix", 300, 30),
                        new MeasureNode(new NodeId("read-1"), "Read", MeasurementType.ABSORBANCE)),
                List.of(new Dependency(new NodeId("mix-1"), new NodeId("read-1"))));
    }

    @Test
    void shouldReturn201WithWorkflowWhenCreated() throws Exception {
        when(workflowCrud.create(any(WorkflowDraft.class))).thenReturn(sampleWorkflow());

        mvc.perform(post("/workflows").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.nodes[0].type").value("MIX"))
                .andExpect(jsonPath("$.nodes[0].speedRpm").value(300))
                .andExpect(jsonPath("$.nodes[1].measurementType").value("ABSORBANCE"))
                .andExpect(jsonPath("$.dependencies[0].to").value("read-1"));
    }

    @Test
    void shouldTranslateRequestIntoDomainDraft() throws Exception {
        when(workflowCrud.create(any(WorkflowDraft.class))).thenReturn(sampleWorkflow());

        mvc.perform(post("/workflows").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated());

        ArgumentCaptor<WorkflowDraft> captor = ArgumentCaptor.forClass(WorkflowDraft.class);
        verify(workflowCrud).create(captor.capture());
        assertThat(captor.getValue().nodes()).containsExactly(
                new MixNode(new NodeId("mix-1"), "Mix", 300, 30),
                new MeasureNode(new NodeId("read-1"), "Read", MeasurementType.ABSORBANCE));
        assertThat(captor.getValue().dependencies())
                .containsExactly(new Dependency(new NodeId("mix-1"), new NodeId("read-1")));
    }

    @Test
    void shouldReturn422WhenNodeParameterIsMissing() throws Exception {
        String request = """
                {"name": "Bad", "nodes": [{"id": "a", "name": "A", "type": "MIX"}], "dependencies": []}
                """;

        mvc.perform(post("/workflows").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.failures[0].code").value("MISSING_PARAMETER"));
        verifyNoInteractions(workflowCrud);
    }

    @Test
    void shouldReturn400WhenRequiredFieldIsMissing() throws Exception {
        mvc.perform(post("/workflows").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nodes\": [], \"dependencies\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(workflowCrud);
    }

    @Test
    void shouldReturn404WhenWorkflowIsMissing() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(workflowCrud.get(id)).thenThrow(new WorkflowNotFoundException(id));

        mvc.perform(get("/workflows/{id}", id.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKFLOW_NOT_FOUND"));
    }

    @Test
    void shouldReturn400WhenIdIsNotAUuid() throws Exception {
        mvc.perform(get("/workflows/{id}", "not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenPublishingFromInvalidState() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(publishWorkflow.publish(id)).thenThrow(new InvalidWorkflowStateException("already published"));

        mvc.perform(post("/workflows/{id}/publish", id.value()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_WORKFLOW_STATE"));
    }

    @Test
    void shouldReturn422WithAllFailuresWhenGraphIsInvalid() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(publishWorkflow.publish(id)).thenThrow(new InvalidWorkflowException(List.of(
                new ValidationFailure("CYCLE_DETECTED", "cycle"),
                new ValidationFailure("DISCONNECTED_GRAPH", "islands"))));

        mvc.perform(post("/workflows/{id}/publish", id.value()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.failures[0].code").value("CYCLE_DETECTED"))
                .andExpect(jsonPath("$.failures[1].code").value("DISCONNECTED_GRAPH"));
    }

    @Test
    void shouldReturn204WhenWorkflowIsDeleted() throws Exception {
        WorkflowId id = WorkflowId.generate();

        mvc.perform(delete("/workflows/{id}", id.value())).andExpect(status().isNoContent());

        verify(workflowCrud).delete(id);
        verify(publishWorkflow, never()).publish(any());
    }
}
