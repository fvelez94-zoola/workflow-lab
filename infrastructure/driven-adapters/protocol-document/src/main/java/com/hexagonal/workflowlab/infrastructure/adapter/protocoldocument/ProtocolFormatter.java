package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;

import com.hexagonal.workflowlab.domain.model.protocol.ProtocolSheet;

/** One implementation per document format. */
interface ProtocolFormatter {

    String format(ProtocolSheet sheet);
}
