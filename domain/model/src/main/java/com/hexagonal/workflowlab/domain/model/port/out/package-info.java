/**
 * Output (driven) ports: what the application needs from the outside world.
 *
 * <p>Owns: the contracts, expressed with domain types only. May depend on: {@code workflow} and
 * {@code experiment}. Invariant: no framework, JPA, JDBC or HTTP type may appear in a signature.
 */
package com.hexagonal.workflowlab.domain.model.port.out;
