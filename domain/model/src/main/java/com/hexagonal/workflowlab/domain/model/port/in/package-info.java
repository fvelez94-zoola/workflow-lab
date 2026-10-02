/**
 * Input (driving) ports: what the application can do, as seen from the outside.
 *
 * <p>Owns: the use case contracts. May depend on: {@code workflow} and {@code experiment}. Invariant:
 * entry points depend on these interfaces, never on the implementations in {@code domain/usecase}.
 */
package com.hexagonal.workflowlab.domain.model.port.in;
