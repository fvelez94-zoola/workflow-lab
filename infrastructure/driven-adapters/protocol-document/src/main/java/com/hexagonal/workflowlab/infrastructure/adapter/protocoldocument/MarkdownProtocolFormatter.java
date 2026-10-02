package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

import com.hexagonal.workflowlab.domain.model.protocol.ProtocolSheet;

final class MarkdownProtocolFormatter implements ProtocolFormatter {

    @Override
    public String format(ProtocolSheet sheet) {
        MarkdownTable table = new MarkdownTable("#", "Node", "Type", "Description");
        sheet.steps().forEach(step -> table.row(
                String.valueOf(step.sequence()), step.sourceNodeId(), step.type().name(), step.description()));
        return "# Protocol: " + sheet.title() + "\n\n" + table.render();
    }
}
