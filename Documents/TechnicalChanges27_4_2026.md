# Technical Changes

## DB credentials
- Database credentials were moved from hardcoded values into environment variables and a db.properties configuration file.

### Reason:
- Hardcoding credentials poses a security risk and makes configuration inflexible across environments.

### Impact:
- Improves security by keeping sensitive data out of source code
- Simplifies deployment across different environments (dev/test/prod)
- Requires environment variables to be set before running the application

## SonarQube properties
- Test files were excluded from SonarQube analysis to improve the accuracy of code coverage metrics.

### Reason:
- Including test files can distort coverage results and reduce the usefulness of quality metrics.

### Impact:
- More accurate representation of production code coverage
- No effect on runtime behavior
- Only affects static analysis reporting

## Code refactoring and Quality improvements
- The codebase was refactored using tools such as SonarQube, Checkstyle, and PMD to improve code quality and maintainability.
- 2 critical issues in bestpractices were fixed
- 7 critical design issues were fixed
- 256 naming convention issues in codestyle fixed

### Reason:
- To enforce consistent coding standards, reduce code smells, and improve long-term maintainability.

### Implementation:
- Addressed issues reported by static analysis tools
- Standardized formatting and naming conventions
- Removed redundant or inefficient code patterns

### Impact:
- Improved readability and maintainability
- Reduced technical debt
- No functional changes to application behavior
