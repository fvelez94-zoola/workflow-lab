/**
 * The protocol document: what is exported, independent of how it is written.
 *
 * <p>Owns: the format-free content ({@code ProtocolSheet}) and the list of supported formats. May depend
 * on: {@code workflow} and {@code experiment}. Invariant: no string building, no escaping, no file names;
 * those are formatting concerns that live in the adapter.
 */
package com.hexagonal.workflowlab.domain.model.protocol;
