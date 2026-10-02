/**
 * Assembly configuration: creates the use case beans and optional sample data.
 *
 * <p>Owns: wiring. May depend on: everything that is a port or a use case. Invariant: never imports an
 * adapter class; adapters are discovered by Spring and injected through the output-port interfaces.
 */
package com.hexagonal.workflowlab.config;
