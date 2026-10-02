/**
 * REST controllers: the driving adapter of the application.
 *
 * <p>Owns: HTTP concerns (status codes, DTOs). May depend on: {@code domain.model} (input ports and
 * domain types) and the generated API. Invariant: must NOT depend on {@code domain.usecase}; the
 * controllers only know the input-port interfaces (enforced by ArchUnit).
 */
package com.hexagonal.workflowlab.infrastructure.entrypoint.rest;
