# API Design Rules

1. **REST Resource Naming:** Use plural nouns for resources (e.g., `/api/workorders`, not `/api/workOrder`).
2. **HTTP Verbs:** Use standard verbs: `POST` for creation, `GET` for retrieval, `PUT`/`PATCH` for updates, `DELETE` for removal.
3. **Strict Schema Conformance:** DO NOT invent extra JSON fields or properties that are not explicitly defined in the requirements or API spec.
4. **Error Responses (RFC 7807):** All error responses must return Problem Details with fields: `type`, `title`, `status`, and `detail`.
5. **Validation:** All incoming request bodies must be annotated with `@Valid` and appropriate constraints (`@NotNull`, `@NotBlank`).

#### Example:
- **[GOOD]:** Returning `ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetails)`
- **[BAD]:** Returning raw strings or generic stack traces on validation failure.
