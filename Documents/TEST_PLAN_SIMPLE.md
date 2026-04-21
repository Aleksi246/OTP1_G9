# Test Plan — Classroom Resource Sharing Platform

**Project:** OTP1_G9 | **Date:** April 21, 2026 | **Stack:** Java 21, Javalin, JavaFX, MariaDB, JUnit 5, Mockito

## Objectives
- Validate core features (authentication, class management, materials, reviews)
- Verify security (password hashing, JWT, authorization)
- Achieve 60% code coverage with JUnit 5
- Ensure multi-language/RTL support

## Test Environment
- **Docker containers:** Javalin backend + MariaDB
- **Testing framework:** JUnit 5 with Mockito
- **Code coverage:** JaCoCo
- **Code quality & security:** SonarQube
- **CI/CD:** Jenkins with Maven Surefire plugin
- **Test data:** Initialized via init.sql script

---

## Functional Tests

### User Authentication
| Test Case | Expected Result |
|-----------|-----------------|
| Register with valid input | Account created, password hashed with BCrypt |
| Register with duplicate email | Error: "Email already exists" |
| Login with correct credentials | JWT token issued, access granted |
| Login with wrong password | 401 Unauthorized, no token |
| Logout | Session cleared, no access to protected resources |

### Class Management
| Test Case | Expected Result |
|-----------|-----------------|
| Create class | Class created with creator as participant |
| Create duplicate named class | Error: "Class already exists" |
| Join existing class | User enrolled, class appears in list |
| Delete class (creator only) | Class + materials + reviews deleted |
| Non-creator attempts delete | 403 Forbidden, deletion blocked |

### Material Management
| Test Case | Expected Result |
|-----------|-----------------|
| Upload material (creator only) | File saved to disk, metadata in DB |
| Non-creator attempts upload | 403 Forbidden, upload blocked |
| Download material (enrolled user) | File downloaded successfully |
| Non-enrolled user attempts download | 403 Forbidden |
| Delete material (creator only) | File + DB record deleted |

### Reviews & Ratings
| Test Case | Expected Result |
|-----------|-----------------|
| Submit review with valid rating (0-5) | Review stored with user/material link |
| Submit review with rating > 5 | Validation error |
| View reviews for material | All reviews displayed with average rating |
| Delete own review | Review removed from DB |

---

## Security Tests

| Test Case | Expected Result |
|-----------|-----------------|
| JWT token tampering | Request rejected with 401 Unauthorized |
| Password stored in DB | Hashed with BCrypt, not plaintext |

---

## Non-Functional Tests

| Area | Acceptance Criteria |
|------|-------------------|
| Multi-language | All 4 languages render correctly, Persian RTL aligned |
| Compatibility | Windows |

---

## Test Execution

**Run all unit tests:**
```bash
mvn test
```

**Run specific test class:**
```bash
mvn test -Dtest=UserAcceptanceTest
```

**Generate JaCoCo code coverage report:**
```bash
mvn test jacoco:report
```
Report location: `target/site/jacoco/index.html`

**Run SonarQube analysis:**
```bash
mvn clean sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=YOUR_TOKEN
```

**Run SonarQube analysis with JaCoCo integration:**
```bash
mvn clean test jacoco:report sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=YOUR_TOKEN
```

---

## Code Quality & Coverage Analysis

### JaCoCo Coverage Metrics
- **Target:** ≥ 60% line coverage
- **Report:** Auto-generated at `target/site/jacoco/index.html`
- **Coverage by module:**

### SonarQube Quality Gates
- **Bugs:** 0 critical, max 3 major
- **Code smells:** max 15
- **Security hotspots:** All reviewed, none exploitable
- **Duplications:** < 3%
- **Maintainability rating:** A
- **Security rating:** A
- **Coverage:** ≥ 60%

---

## Test Files Location
- **Unit tests:** `src/test/java/`
- **Integration tests:** `com/example/otp/controllers/UserAcceptanceTest.java`
- **DAO tests:** `com/example/otp/dao/MaterialDaoTest.java`
- **Controller tests:** `com/example/app/ClassControllerLogicTest.java`, `HomeControllerLogicTest.java`

---

## Acceptance Criteria – Pass/Fail
✓ All unit tests passing (100%)  
✓ Code coverage ≥ 60% (JaCoCo)  
✓ SonarQube quality gate passed  
✓ Zero critical bugs (SonarQube)  
✓ Zero critical security issues  
✓ Security rating: A (SonarQube)  
✓ Maintainability rating: A (SonarQube)  
✓ Multi-language support verified  
✓ Database cascade delete working  


