# Sprint 4 Review Report

## Sprint Goal

Finalize product functionality, validate Docker-based delivery, and prepare the system for a shareable demonstration build.

## Review Summary

Sprint 4 focused on converting the feature-complete prototype into a reproducible packaged system. The main review outcome was that the application could be built into a Docker image, started together with MariaDB through Docker Compose, and documented for development and demonstration use.

## Completed Work

- Finalized the runnable application flow for demonstration.
- Added containerization assets through the repository Docker configuration.
- Prepared compose-based startup for the application and database services.
- Continued documentation updates needed for final presentation and later sprint deliverables.

## Evidence in Repository

- [Dockerfile](../../Dockerfile)
- [docker-compose.yml](../../docker-compose.yml)
- [init.sql](../../init.sql)
- [Jenkinsfile](../../Jenkinsfile)

## What Went Well

- Docker packaging created a more reproducible setup across environments.
- The application and database responsibilities were clearly separated into services.
- The repository gained stronger deployment documentation foundations for later sprints.

## What Could Be Improved

- Production-oriented container hardening and orchestration were still limited.
- Final documentation could have been prepared earlier to reduce end-of-project consolidation work.

## Next Sprint Focus

Implement UI localization, improve multilingual behavior, and continue documentation updates for scalability-oriented features.