/**
 * Mapping and presentation helpers of the REST layer.
 *
 * <p>Owns: DTO-to-domain translation (one mapper per concern), HTTP-shape validation of node parameters
 * ({@code RequiredParameters}), and presentation details ({@code DurationFormatter},
 * {@code ProtocolMediaType}). May depend on: the domain model and the generated DTOs. Invariant: business
 * rules are never decided here, and none of these helpers is reachable from the domain.
 */
package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.mapper;
