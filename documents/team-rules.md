# Team Java Rules

## Security
- Never build SQL by string concatenation. Use PreparedStatement or Spring's JdbcTemplate with parameters.
- Never log passwords, tokens or personal data.
- Do not catch and silently swallow exceptions. Log them or rethrow.

## Maintainability
- Methods with cyclomatic complexity above 10 must be split into smaller methods.
- Prefer constructor injection over field injection.
- Use try-with-resources for every Closeable (connections, streams, readers).

## Testing
- Every public method needs a JUnit 5 test using AssertJ assertions.