# Sprint 6 Review Report

**Goal:** Database localization, code review & refactoring, acceptance test planning.  
**Duration:** 2 weeks

---

## Completed Tasks

| Task | Points | Summary |
|------|--------|---------|
| Database Localization | 3 | `localization_strings` table (utf8mb4), EN/FR/RU/FA, fallback to English |
| Statistical Code Review | 3 | SonarQube analysis — complexity, duplication, and violations documented |
| Code Clean-Up & Refactoring | 2 | Refactored methods, removed duplicates, all tests pass |
| Acceptance Test Planning | 1 | Test cases and coverage matrix in [Acceptance_Test_Plan.md](../Acceptance_Test_Plan.md) |
| Architecture Documentation | 1 | Updated ER diagram in `Documents/diagrams/` |

## UI Localization

All UI text driven from DB. Dynamic language switching and RTL (Persian) supported.

## Backlog Update

Sprint and Product Backlogs updated with Sprint 6 tasks and acceptance criteria.
https://trello.com/b/siutFRge/sprint-6

## GitHub Update

All deliverables committed: localization code, refactored codebase, test plan, diagrams, and this report.

## Contributions

| Name | Tasks | Hours | Weekly Tasks |
|------|-------|-------|--------------|
| Elias Eide | DB localization, code review, test planning | 20 | Submitted |
| Aleksi Lappalainen | DB localization, clean-up, architecture docs | 20 | Submitted |
| Arttu Salo | Code review, refactoring, test planning | 20 | Submitted |
| Eero Koivukoski | Clean-up, diagrams, backlog updates | 5 | Not Submitted |

## What Went Well

- Localization strategy defined early — smooth implementation.
- SonarQube gave clear, actionable metrics.

## What Could Be Improved

- Refactoring delayed by code review dependency.
- SonarQube should be integrated into CI/CD earlier.

## Next Sprint Focus

Execute acceptance tests, fix remaining analysis findings, finalize project delivery.
