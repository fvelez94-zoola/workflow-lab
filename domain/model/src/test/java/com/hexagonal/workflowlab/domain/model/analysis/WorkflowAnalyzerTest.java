package com.hexagonal.workflowlab.domain.model.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkflowAnalyzerTest {

    private final WorkflowAnalyzer analyzer =
            new WorkflowAnalyzer(new CriticalPathCalculator(new NodeDurationPolicy()));

    @Test
    void shouldCombineNodeCountDurationAndCriticalPath() {
        Workflow workflow = Workflow.draft("Assay", null,
                List.of(new MixNode(new NodeId("a"), "Mix A", 100, 10), new MixNode(new NodeId("b"), "Mix B", 100, 20)),
                List.of(new Dependency(new NodeId("a"), new NodeId("b"))));

        WorkflowAnalysis analysis = analyzer.analyze(workflow);

        assertThat(analysis.workflowId()).isEqualTo(workflow.getId());
        assertThat(analysis.nodeCount()).isEqualTo(2);
        assertThat(analysis.estimatedDuration()).isEqualTo(Duration.ofSeconds(30));
        assertThat(analysis.criticalPath()).extracting(CriticalPathStep::nodeName).containsExactly("Mix A", "Mix B");
    }
}
