# Security Rules

1. **No Hardcoded Secrets:** Never hardcode API keys, passwords, connection strings, or tokens in source code or comments. Use environment variables.
2. **Input Sanitization & Validation:** Always validate and sanitize inputs at the controller boundary. Never trust client payloads.
3. **SQL / Injection Protection:** Use Spring Data JPA / Hibernate parameterized queries or ORM methods. 
Never concatenate strings to build native SQL or JPQL queries.
4. **Authorization Checks:** Ensure proper role-based access control (RBAC) is declared on endpoints (e.g., `@PreAuthorize("hasRole('TECHNICIAN')")`).
