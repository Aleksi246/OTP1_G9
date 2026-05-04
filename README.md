# Classroom Resource Sharing Platform (OTP1_G9)

## 1. Project Title & Overview
Classroom Resource Sharing Platform is a Java-based learning system for managing courses, materials, and peer feedback in one workflow.
The project solves fragmented file sharing and review processes in course environments by centralizing class enrollment, uploads, downloads, and ratings.
Target users are students and instructors who need role-based access to classroom resources.
The system combines a JavaFX desktop UI, Javalin REST backend, and MariaDB persistence.
Security includes JWT-based authorization and BCrypt password hashing.
The implementation also supports multilingual UI behavior including RTL handling for Persian.
Project duration was 8 sprints, each 2 weeks (total 16 weeks).

## 2. Product Vision
### Vision Statement
Build a secure, testable, and multilingual classroom platform where course resources can be shared and reviewed efficiently.

### Main Goals
- Centralize course resource management.
- Enforce role-based behavior for creators and participants.
- Provide measurable software quality through automated testing and analysis.
- Support localization at both UI and database levels.

### Key Features
- User registration, login, and password change.
- Course creation, enrollment, update, and deletion.
- File upload/download with authorization checks.
- Material reviews and ratings.
- Multilingual UI (English, French, Russian, Persian) with RTL support.
- Dockerized execution and CI pipeline automation.

### Definition of Success
The project is considered complete when core user stories are implemented, automated tests pass, sprint deliverables are documented, quality metrics are available, and the system can be run reproducibly via local Maven and Docker workflows.

## 3. Project Plan & Sprint Structure
### Development Methodology
The team followed Agile/Scrum with sprint planning, sprint review, and backlog updates.

### Sprint Length
Each sprint lasted 2 weeks.

### 8-Sprint Timeline (Goals)
- Sprint 1: Project kickoff, vision, and initial planning.
- Sprint 2: Requirements, database foundation, initial UI/testing setup.
- Sprint 3: Core feature completion, CI and coverage automation.
- Sprint 4: Product finalization and Docker packaging.
- Sprint 5: UI localization and internationalization.
- Sprint 6: Database localization and quality/refactoring work.
- Sprint 7: QA execution, acceptance testing, and issue tracking.
- Sprint 8: Documentation consolidation and final delivery packaging.

## 4. Sprint 1 - Project Planning & Vision
### Summary
Sprint 1 established project direction, initial scope, and startup coordination.

### Included Work
- Project plan summary and project vision definition.
- Initial backlog direction and next-sprint preparation.
- Early risk/scope recognition (team communication and planning improvements identified).

### Planning/Review Links
- Sprint planning report: [Sprint 1 Planning](Documents/sprint_reports/sprint1_planning_report.md)
- Sprint review report: [Sprint 1 Review](Documents/sprint_reports/sprint1_review_report.md)

## 5. Sprint 2 - Requirements & Database
### Functional Requirements Summary
Sprint 2 focused on requirements implementation foundations: schema design, initial UI work, unit tests, and code coverage setup.

### Use Case Diagram
- [Use Case Diagram](Documents/diagrams/img_1.png)

### ER Diagram
- [ER Diagram](Documents/diagrams/ER.png)

### Database Technology
- MariaDB with relational schema and constraints.

### Database Implementation Overview
- Core tables and relationships created and validated.
- CRUD behavior tested during sprint.

### Unit Testing Strategy and Tools
- JUnit 5 + Mockito with Maven Surefire.
- JaCoCo introduced for coverage tracking.

### Planning/Review Links
- Sprint planning report: [Sprint 2 Planning](Documents/sprint_reports/sprint2_planning_report.md)
- Sprint review report: [Sprint 2 Review](Documents/sprint_reports/sprint2_review_report.md)

## 6. Sprint 3 - UI Implementation & CI
### UI Framework and Design Approach
- JavaFX + FXML + CSS for desktop UI.
- Incremental integration of UI and backend features.

### Screens Implemented
- Login and registration flow.
- Home/class management views.
- Material and review dialogs.

### Code Coverage Goals and Tools
- Expanded unit and integration testing.
- JaCoCo pipeline publication through Jenkins.

### Jenkins Pipeline
- Build stage.
- Test stage.
- Coverage report stage.

### Planning/Review Links
- Sprint planning report: [Sprint 3 Planning](Documents/sprint_reports/sprint3_planning_report.md)
- Sprint review report: [Sprint 3 Review](Documents/sprint_reports/sprint3_review_report.md)

## 7. Sprint 4 - Docker Containerization
### Purpose of Docker in the Project
Docker was used to standardize runtime setup, simplify environment bootstrapping, and prepare reproducible delivery.

### Services Containerized
- Application service.
- MariaDB database service.

### Dockerfile and Compose Overview
- [Dockerfile](Dockerfile)
- [Docker Compose](docker-compose.yml)

### Container Usage in Development/Testing
- Local development and smoke validation run through compose stack.
- Database bootstrap is wired with [init.sql](init.sql).

### Planning/Review Links
- Sprint planning report: [Sprint 4 Planning](Documents/sprint_reports/sprint4_planning_report.md)
- Sprint review report: [Sprint 4 Review](Documents/sprint_reports/sprint4_review_report.md)

## 8. Sprint 5 - UI Localization & Kubernetes
### Supported UI Languages
- English (`en`)
- French (`fr`)
- Russian (`ru`)
- Persian (`fa`, RTL)

### Localization Approach
- UI text externalized and dynamically selected by locale.
- Runtime language switching with RTL handling for Persian.

### Kubernetes Usage
Kubernetes manifests are not included in this repository.
- Deployment strategy: not applied in this project repository.
- Services/scaling: not applied in this project repository.

### Planning/Review Links
- Sprint planning report: [Sprint 5 Planning](Documents/sprint_reports/sprint5_planning_report.md)
- Sprint review report: [Sprint 5 Review](Documents/sprint_reports/sprint5_review_report.md)

## 9. Sprint 6 - Database Localization
### Language/Region-Specific Data Handling
- Localization moved to database layer using key-value translation entries.
- UTF-8 capable schema for multilingual strings.

### Migration or Schema Changes
- Added `localization_strings` table and uniqueness rule on `(translation_key, language)`.
- Seeded multilingual content through [init.sql](init.sql).

### Validation Approach
- Static key checks against seed data.
- Compile/test validation after localization updates.
- Runtime fallback to English for missing localized keys.

### Planning/Review Links
- Sprint planning report: [Sprint 6 Planning](Documents/sprint_reports/sprint6_planning_report.md)
- Sprint review report: [Sprint 6 Review](Documents/sprint_reports/sprint6_review_report.md)
- Detailed implementation report: [Database Localization Report](Documents/DATABASE_LOCALIZATION_REPORT.md)

## 10. Sprint 7 - Quality Assurance
### SonarQube Usage and Metrics
- SonarQube/SonarScanner used for static quality and security checks.
- Supporting metrics and findings documented in:
  - [Statistical Code Review](Documents/STATISTICAL_CODE_REVIEW.MD)
  - [Technical Changes](Documents/TechnicalChanges27_4_2026.md)

### Code Quality Goals
- Reduce critical findings, improve naming/standards consistency, and lower technical debt through refactoring.

### JMeter Test Scenarios
JMeter artifacts are not included in this repository.

### Functional and Non-Functional Testing
- Functional, UAT, and acceptance coverage documented in:
  - [Sprint 7 Deliverable Report](Documents/Sprint7_Deliverable_Report.md)
  - [Acceptance Test Plan](Documents/Acceptance_Test_Plan.md)
  - [Test Plan](Documents/TEST_PLAN_SIMPLE.md)
  - [Test Cases](Documents/TEST_CASES.md)

### Planning/Review Links
- Sprint planning report: [Sprint 7 Planning](Documents/sprint_reports/sprint7_planning_report.md)
- Sprint review report: [Sprint 7 Deliverable Report](Documents/Sprint7_Deliverable_Report.md)

## 11. Sprint 8 - Documentation & Finalization
### Technical Documentation
- Sprint reports and technical notes consolidated in [Documents](Documents/).
- DB initialization behavior and safety modes documented in [DB Init Guide](Documents/DB_INIT_GUIDE.md).
- Code quality and localization details are documented in [Statistical Code Review](Documents/STATISTICAL_CODE_REVIEW.MD), [Technical Changes](Documents/TechnicalChanges27_4_2026.md), and [Database Localization Report](Documents/DATABASE_LOCALIZATION_REPORT.md).

### User Documentation
- End-user testing flows and acceptance scenarios documented in:
  - [Acceptance Test Plan](Documents/Acceptance_Test_Plan.md)
  - [Test Cases](Documents/TEST_CASES.md)
  - [Test Plan](Documents/TEST_PLAN_SIMPLE.md)

### API Documentation
- API call examples are provided in [HTTP Test Suite](http_test.http).

### Final System Architecture Diagram
- [Class/Architecture Diagram](Documents/diagrams/LuokkakaavioGroup9-1-1.png)

### Planning/Review Links
- Sprint planning report: [Sprint 8 Planning](Documents/sprint_reports/sprint8_planning_report.md)
- Sprint review report: [Sprint 8 Review](Documents/sprint_reports/sprint8_review_report.md)

## 12. How to Run the Project
### Prerequisites
- Java 21
- Maven 3.6+
- Docker + Docker Compose (for containerized run)
- MariaDB (if running without Docker)

### Environment Setup
1. Configure database credentials using environment variables or properties as described in [DB Init Guide](Documents/DB_INIT_GUIDE.md).
2. Ensure initialization SQL file is available: [init.sql](init.sql).
3. If you are running without Docker, provide a MariaDB instance and matching `DB_URL`, `DB_USER`, and `DB_PASS` values for the application.

### Run with Maven
```bash
mvn clean javafx:run
```

### Run with Docker Compose
```bash
docker compose up --build
```

### Optional Stop Command
```bash
docker compose down
```

### How to Access the Application
- Desktop application: starts through JavaFX when launched with Maven or inside the app container.
- Backend/API base URL: `http://localhost:7700`
- API request examples: [HTTP Test Suite](http_test.http)

## 13. Testing Instructions
### Run Unit/Integration Tests
```bash
mvn test
```

### Generate Coverage Report
```bash
mvn test jacoco:report
```
Coverage report entry point:
- [JaCoCo HTML Index](target/site/jacoco/index.html)

### Performance Testing Instructions
- Performance criteria and scenarios are documented in [Test Plan](Documents/TEST_PLAN_SIMPLE.md).
- Dedicated JMeter files are not included in this repository.

## 14. Repository Structure
Main directories and files:
- [src/main](src/main) - Application source: UI, controllers, DAO, models, and services
- [src/test](src/test) - Unit and integration tests
- [Documents](Documents) - Sprint reports, test plans, technical reports, diagrams
- [Documents/diagrams](Documents/diagrams) - Use case, ER, architecture, flow visuals
- [uploads](uploads) - Uploaded material storage during runtime/tests
- [docker-compose.yml](docker-compose.yml) - Multi-service container orchestration
- [Dockerfile](Dockerfile) - Application image definition
- [Jenkinsfile](Jenkinsfile) - CI pipeline stages
- [init.sql](init.sql) - Database schema and seed data

## 15. Authors
### Team Members and Roles
- Elias Eide - Development, Testing, Documentation
- Aleksi Lappalainen - CI/CD, DevOps, Quality Reporting
- Arttu Salo - Development, Testing, Quality Checks
- Eero Koivukoski - QA Support, Issue Tracking, Documentation Support

### Course
- AD / Spring 2026
- Project: OTP2 / Team OTP1_G9
