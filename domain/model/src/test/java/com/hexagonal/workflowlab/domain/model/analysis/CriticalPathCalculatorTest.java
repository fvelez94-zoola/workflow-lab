package com.hexagonal.workflowlab.domain.model.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The calculator is pure logic over the real domain objects: no mocks needed. */
class CriticalPathCalculatorTest {

    private final CriticalPathCalculator calculator = new CriticalPathCalculator(new NodeDurationPolicy());

    private static MixNode mix(String id, int seconds) {
        return new MixNode(new NodeId(id), "Mix " + id, 100, seconds);
    }

    private static Dependency dependency(String from, String to) {
        return new Dependency(new NodeId(from), new NodeId(to));
    }

    private static List<String> ids(CriticalPath path) {
        return path.steps().stream().map(step -> step.nodeId().value()).toList();
    }

    @Test
    void shouldSumDurationsOfASequentialChain() {
        Workflow workflow = Workflow.draft("Chain", null,
                List.of(mix("a", 10), new IncubateNode(new NodeId("b"), "Incubate", 37, 1),
                        new MeasureNode(new NodeId("c"), "Read", MeasurementType.ABSORBANCE)),
                List.of(dependency("a", "b"), dependency("b", "c")));

        CriticalPath path = calculator.calculate(workflow);

        assertThat(path.totalDuration()).isEqualTo(Duration.ofSeconds(10 + 60 + 30));
        assertThat(ids(path)).containsExactly("a", "b", "c");
    }

    @Test
    void shouldFollowTheSlowestBranchOfADiamond() {
        Workflow workflow = Workflow.draft("Diamond", null,
                List.of(mix("start", 10), mix("slow", 100), mix("fast", 20), mix("end", 5)),
                List.of(dependency("start", "slow"), dependency("start", "fast"),
                        dependency("slow", "end"), dependency("fast", "end")));

        CriticalPath path = calculator.calculate(workflow);

        assertThat(path.totalDuration()).isEqualTo(Duration.ofSeconds(115));
        assertThat(ids(path)).containsExactly("start", "slow", "end");
    }

    @Test
    void shouldPickTheLongestOfIndependentBranches() {
        Workflow workflow = Workflow.draft("Islands", null, List.of(mix("short", 10), mix("long", 50)), List.of());

        CriticalPath path = calculator.calculate(workflow);

        assertThat(path.totalDuration()).isEqualTo(Duration.ofSeconds(50));
        assertThat(ids(path)).containsExactly("long");
    }

    @Test
    void shouldBreakTiesByDeclarationOrder() {
        Workflow workflow = Workflow.draft("Tie", null, List.of(mix("first", 30), mix("second", 30)), List.of());

        assertThat(ids(calculator.calculate(workflow))).containsExactly("first");
    }

    @Test
    void shouldReturnZeroDurationForEmptyWorkflow() {
        CriticalPath path = calculator.calculate(Workflow.draft("Empty", null, List.of(), List.of()));

        assertThat(path.totalDuration()).isEqualTo(Duration.ZERO);
        assertThat(path.steps()).isEmpty();
    }

    @Test
    void shouldFailWhenGraphHasCycle() {
        Workflow workflow = Workflow.draft("Cycle", null, List.of(mix("a", 10), mix("b", 10)),
                List.of(dependency("a", "b"), dependency("b", "a")));

        assertThatThrownBy(() -> calculator.calculate(workflow)).isInstanceOf(InvalidWorkflowException.class);
    }
}
