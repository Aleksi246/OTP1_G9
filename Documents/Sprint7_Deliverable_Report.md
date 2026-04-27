# Sprint 7 Deliverable Report

Project: OTP2/AD (2026)  
Team: OTP1_G9  
Sprint: 7 (Testing)  
Date: 27.04.2026

## 1. Summary

In Sprint 7, we focused on testing and final quality checks. We ran automated tests, reviewed usability, tracked bugs, and updated project documents.

## 2. Functional Tasks

### 2.1 Test Plan
- Test plan created with objective, resources, environment, and tasks.
- Main areas: authentication, class management, materials, reviews, and security/localization checks.

Reference: Documents/TEST_PLAN_SIMPLE.md

### 2.2 Unit and Integration Testing
- Total tests run: 391
- Failures: 0
- Errors: 0
- Skipped: 0
- UserAcceptanceTest: 78/78 passed
- ApiEndpointsIntegrationTest: 7/7 passed

References:
- target/surefire-reports/
- target/surefire-reports/com.example.otp.controllers.UserAcceptanceTest.txt
- target/surefire-reports/com.example.otp.controllers.ApiEndpointsIntegrationTest.txt

### 2.3 Bug and Issue Tracking
- BUG-001 (UI resize issue): Open, Low
- BUG-002 (Jenkins missing config files): Resolved, Critical

Reference: Documents/BUGS_AND_ISSUES_TRACKER.md

### 2.4 User Stories and CI/CD
- User stories were reviewed against latest behavior and test results.
- Jenkins pipeline includes build, test, JaCoCo, SonarQube, and Docker image steps.

References:
- Documents/Acceptance_Test_Plan.md
- Documents/TEST_CASES.md
- Jenkinsfile
- docker-compose.yml

## 3. Non-functional Tasks

### 3.1 Heuristic Evaluation
- Main issues found: validation/error feedback, error message clarity, and window behavior.

Reference: Documents/diagrams/Heuristic Summary OTP2-1.png

### 3.2 UAT
- UAT scenarios were executed for login, classes, materials, reviews, and logout.
- Automated UAT suite result: 78/78 passed.

References:
- Documents/Acceptance_Test_Plan.md
- Documents/TEST_CASES.md

### 3.3 Security and Performance
- Security checks included JWT authorization behavior and BCrypt password hashing.
- CI/CD secrets handling improved by moving credentials out of source control.
- Performance testing was limited; no critical blocker observed in current runs.

References:
- Documents/TechnicalChanges27_4_2026.md
- Documents/TEST_PLAN_SIMPLE.md

## 4. Quality Snapshot

- Test pass rate: 391/391
- JaCoCo line coverage: 93.18% (1079/1158)
- Branch coverage: 78.48% (372/474)

Reference: target/site/jacoco/jacoco.csv

## 5. Contributions

| Team Member Name | Assigned Tasks | Time Spent (hrs) | In-class Tasks |
|---|---|---:|---|
| Elias Eide | Test execution, UAT support, documentation | 20 | Submitted |
| Aleksi Lappalainen | CI/CD and SonarQube verification, reporting | 20 | Submitted |
| Arttu Salo | Functional testing, bug tracking, quality checks | 20 | Submitted |
| Eero Koivukoski | Heuristic review support and issue tracking | 5 | Not Submitted |

## 6. Appendix

- Documents/TEST_PLAN_SIMPLE.md
- Documents/Acceptance_Test_Plan.md
- Documents/TEST_CASES.md
- Documents/BUGS_AND_ISSUES_TRACKER.md
- Documents/TechnicalChanges27_4_2026.md
- Documents/STATISTICAL_CODE_REVIEW.MD
- Documents/diagrams/Heuristic Summary OTP2-1.png
- Jenkinsfile
- docker-compose.yml
