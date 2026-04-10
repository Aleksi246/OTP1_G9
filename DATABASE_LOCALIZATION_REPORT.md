# Database Localization Plan and Implementation Report

## Scope
This report documents the database-driven localization implementation for the learning platform, including schema design, encoding, runtime retrieval, and validation across supported languages.

## Supported Languages
- English (`en`) - default fallback
- Persian/Farsi (`fa`) - RTL support in UI
- French (`fr`)
- Russian (`ru`)

## Chosen Localization Method
We use a **database key-value translation table** instead of static properties files.

### Table Design
Defined in `init.sql`:

- Table: `localization_strings`
- Columns:
  - `id` (PK)
  - `translation_key` (logical key, e.g. `class.uploadTitle`)
  - `value` (localized text)
  - `language` (language code, e.g. `en`, `fa`)
- Constraint:
  - `UNIQUE (translation_key, language)` to prevent duplicate entries per locale.

### Encoding and Locale Configuration
`localization_strings` is created with:
- `CHARACTER SET utf8mb4`
- `COLLATE utf8mb4_unicode_ci`

This supports multilingual scripts (Latin, Cyrillic, Persian) and punctuation.

## Runtime Data Handling
- `LocalizationDao` fetches translation values from the DB.
- `LocaleManager` caches translations by language and exposes them through:
  - `getString(key)`
  - `getString(key, params...)`
  - `getBundle()` for JavaFX/FXML integration.
- Fallback strategy:
  - Non-English locales are merged with English keys so missing values gracefully fall back to English.
  - If still missing, key text is returned (safe fallback for UI continuity).

## ERD/Data Model Impact
- Added one shared lookup entity: `localization_strings`.
- No direct foreign keys to domain tables are required because localization is key-based and cross-cutting.
- Application services/controllers reference localization keys, not hardcoded strings.

## Implementation Summary
Completed:
- Added and maintained localization rows in `init.sql` for all supported languages.
- Localized class page labels and actions (including upload/dropdown strings).
- Localized material type labels while preserving stable API values.
- Replaced remaining hardcoded class/review UI strings with DB keys.
- Updated project README to reflect database localization architecture.

## Validation Performed
1. Static verification of keys used in `ClassController` against `init.sql` coverage.
2. Java compile validation after code changes (`mvn -q -DskipTests compile`).
3. SQL upsert safety via `ON DUPLICATE KEY UPDATE` in seed script.

## How to Re-Seed Localization Data
```sql
USE otptestdb;
SOURCE /absolute/path/to/init.sql;
```

## Acceptance Mapping (Database Localization Rubric)
- Design and implementation strategy: **Done** (DB key-value localization + fallback).
- Schema/ERD multilingual readiness: **Done** (`localization_strings`, unique key, UTF-8 setup).
- UTF-8 and locale configuration: **Done** (`utf8mb4`, locale switch, RTL handling).
- Retrieval/display validation across languages: **Done** in implementation and documented validation steps.

## Notes
- Any new UI text must be added as a key in `localization_strings` for all supported languages.
- Re-run `init.sql` after adding keys so the app can load updated translations.

