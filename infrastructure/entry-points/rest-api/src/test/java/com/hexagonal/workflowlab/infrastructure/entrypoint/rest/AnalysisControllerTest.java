package com.hexagonal.workflowlab.infrastructure.entrypoint.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hexagonal.workflowlab.domain.model.analysis.CriticalPathStep;
import com.hexagonal.workflowlab.domain.model.analysis.WorkflowAnalysis;
import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.domain.model.port.in.AnalyzeWorkflow;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.error.GlobalExceptionHandler;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    @Mock
    private AnalyzeWorkflow analyzeWorkflow;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AnalysisController(analyzeWorkflow))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnDurationBothInSecondsAndAsHumanText() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(analyzeWorkflow.analyze(id)).thenReturn(new WorkflowAnalysis(id, 2, Duration.ofSeconds(4530),
                List.of(new CriticalPathStep(new NodeId("mix"), "Mix", Duration.ofSeconds(30)),
                        new CriticalPathStep(new NodeId("inc"), "Incubate", Duration.ofMinutes(75)))));

        mvc.perform(get("/workflows/{id}/analysis", id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeCount").value(2))
                .andExpect(jsonPath("$.estimatedDurationSeconds").value(4530))
                .andExpect(jsonPath("$.estimatedDuration").value("1h 15m 30s"))
                .andExpect(jsonPath("$.criticalPath[0].nodeId").value("mix"))
                .andExpect(jsonPath("$.criticalPath[1].durationSeconds").value(4500));
    }

    @Test
    void shouldReturn404WhenWorkflowIsMissing() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(analyzeWorkflow.analyze(id)).thenThrow(new WorkflowNotFoundException(id));

        mvc.perform(get("/workflows/{id}/analysis", id.value())).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn422WhenWorkflowHasCycle() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(analyzeWorkflow.analyze(id))
                .thenThrow(InvalidWorkflowException.of("CYCLE_DETECTED", "The workflow contains a dependency cycle"));

        mvc.perform(get("/workflows/{id}/analysis", id.value()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.failures[0].code").value("CYCLE_DETECTED"));
    }
}
