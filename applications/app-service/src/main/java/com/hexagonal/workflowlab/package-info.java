/**
 * Root of the application: the only {@code main()} and the starting point of component scanning.
 *
 * <p>Owns: bootstrapping the Spring context and the beans that cannot carry an annotation. May depend on:
 * the domain model and use cases at compile time; adapters are runtime-only. Invariant: no business logic
 * and no adapter class is ever referenced from here.
 */
package com.hexagonal.workflowlab;
