package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

import com.hexagonal.workflowlab.domain.model.protocol.ProtocolSheet;

final class CsvProtocolFormatter implements ProtocolFormatter {

    @Override
    public String format(ProtocolSheet sheet) {
        StringBuilder csv = new StringBuilder("sequence,node,type,description\n");
        sheet.steps().forEach(step -> csv
                .append(step.sequence()).append(',')
                .append(CsvEscaper.escape(step.sourceNodeId())).append(',')
                .append(step.type().name()).append(',')
                .append(CsvEscaper.escape(step.description())).append('\n'));
        return csv.toString();
    }
}
