package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.generated.model.NodeTypeDto;

/**
 * Validation of the HTTP shape of a node: the OpenAPI schema cannot say "speedRpm is required only when
 * type is MIX", so it is checked here, before any domain object is built. Whether the VALUES make sense
 * (for example speedRpm &gt; 0) is a business rule and stays in the domain.
 */
final class RequiredParameters {

    private RequiredParameters() {
    }

    static <T> T require(T value, String parameter, NodeTypeDto type) {
        if (value == null) {
            throw InvalidWorkflowException.of("MISSING_PARAMETER",
                    "Nodes of type %s require the parameter '%s'".formatted(type, parameter));
        }
        return value;
    }
}
