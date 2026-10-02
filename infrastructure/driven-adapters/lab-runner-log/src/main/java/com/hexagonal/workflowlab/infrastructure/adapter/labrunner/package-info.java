/**
 * Driven adapter that implements the run submission port by logging.
 *
 * <p>Owns: how an experiment is "sent" to a lab. May depend on: the domain model, Spring and SLF4J.
 * Invariant: must not know about the REST layer, the use cases, or the other driven adapters.
 */
package com.hexagonal.workflowlab.infrastructure.adapter.labrunner;
