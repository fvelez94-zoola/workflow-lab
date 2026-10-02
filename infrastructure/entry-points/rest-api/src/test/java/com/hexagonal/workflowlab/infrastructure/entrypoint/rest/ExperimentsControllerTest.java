package com.hexagonal.workflowlab.infrastructure.entrypoint.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.port.in.ConvertWorkflowToExperiment;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ExperimentsControllerTest {

    @Mock
    private ConvertWorkflowToExperiment convertWorkflowToExperiment;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ExperimentsController(convertWorkflowToExperiment))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn201WithNumberedInstructionsInExecutionOrder() throws Exception {
        Workflow workflow = Workflow.draft("Assay", null,
                List.of(new MixNode(new NodeId("mix"), "Mix", 300, 30),
                        new MeasureNode(new NodeId("read"), "Read", MeasurementType.FLUORESCENCE)),
                List.of(new Dependency(new NodeId("mix"), new NodeId("read"))));
        workflow.publish();
        Experiment experiment = Experiment.fromWorkflow(workflow, Instant.parse("2026-10-02T10:00:00Z"));
        experiment.markSubmitted("RUN-7");
        when(convertWorkflowToExperiment.convert(workflow.getId())).thenReturn(experiment);

        mvc.perform(post("/workflows/{id}/experiments", workflow.getId().value()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.runReference").value("RUN-7"))
                .andExpect(jsonPath("$.instructions[0].sequence").value(1))
                .andExpect(jsonPath("$.instructions[0].type").value("MIX"))
                .andExpect(jsonPath("$.instructions[1].sequence").value(2))
                .andExpect(jsonPath("$.instructions[1].description").value("Measure fluorescence"));
    }

    @Test
    void shouldReturn409WhenWorkflowIsNotPublished() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(convertWorkflowToExperiment.convert(id)).thenThrow(new InvalidWorkflowStateException("draft"));

        mvc.perform(post("/workflows/{id}/experiments", id.value()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_WORKFLOW_STATE"));
    }
}
