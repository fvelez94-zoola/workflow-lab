/**
 * Translation of domain exceptions into HTTP error responses.
 *
 * <p>Owns: status codes and the error body. May depend on: domain exceptions and the generated DTOs.
 * Invariant: the domain never decides an HTTP status; it only raises meaningful exceptions.
 */
package com.hexagonal.workflowlab.infrastructure.entrypoint.rest.error;
