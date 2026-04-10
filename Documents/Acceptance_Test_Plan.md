# Acceptance Test Plan — Classroom Resource Sharing Platform

**Project:** OTP1_G9  
**Date:** 10.4.2026  
**Stack:** JavaFX (GUI), Javalin (backend API), MariaDB, JWT + BCrypt for auth

## Overview

This document covers the acceptance test planning for our resource sharing platform. The app lets teachers and students share study materials (textbooks, notes, presentations etc.), organize them into classes by topic, and leave ratings/reviews. Below we go through the acceptance criteria based on our user stories, and then list out the test cases we'd run to verify everything works.

## Acceptance Criteria

These come from the 11 main user stories we've been working with throughout the sprints:

1. **Registration** — New user creates account with username, email, password. Password gets hashed with BCrypt. If the username or email is already taken, it should fail with an error.
2. **Login** — User logs in with email + password and gets back a JWT token. Wrong credentials → error, no token.
3. **Create a class** — Logged-in user creates a class (name + topic). They get added as a participant automatically. Can't create two classes with the same name.
4. **Upload material** — Only the class creator can upload files. You pick a category/material type when uploading. File gets saved to `uploads/` with a UUID name, metadata goes to the DB.
5. **Join a class** — Any logged-in user can join an existing class. Gets added to the participants table.
6. **Download material** — You can only download files from a class you're enrolled in. If you're not in the class → 403.
7. **Review material** — Enrolled users can leave a review (text + rating from 0 to 5). Rating outside that range should be rejected.
8. **View reviews** — Anyone in the class can see all reviews/ratings for a material. Creator especially needs this to see feedback.
9. **Delete material** — Class creator can remove materials. File + DB record both get cleaned up.
10. **Delete class** — Creator deletes the whole class. Everything related (participants, materials, reviews) gets cascade-deleted.
11. **Logout** — User logs out, token is cleared client-side, can't access protected views anymore.

## Coverage Matrix

This maps each test case to the criteria and shows which parts of the code it touches:

| Test | Criteria | What's being tested |
|------|----------|---------------------|
| Register (valid input) | 1 | New user signs up successfully |
| Register (duplicate) | 1 | Try registering with taken username/email |
| Login (correct credentials) | 2 | User logs in and gets a token |
| Login (wrong password) | 2 | Wrong password gets rejected |
| Create class | 3 | Make a new class |
| Create class (duplicate name) | 3 | Try making a class with a name that exists |
| Upload material | 4 | Creator uploads a file |
| Upload (non-creator) | 4 | Someone who isn't the creator tries to upload |
| Join class | 5 | User joins an existing class |
| Download (enrolled) | 6 | Enrolled user downloads a file |
| Download (not enrolled) | 6 | Non-member tries to download |
| Submit review | 7 | Leave a review + rating |
| Review (bad rating) | 7 | Rating outside 0–5 |
| View reviews | 8 | See all reviews for a material |
| Delete material | 9 | Creator removes a file |
| Delete class | 10 | Creator deletes class (cascade) |
| Logout | 11 | Log out and lose access |
| Registration & login UI | 1, 2 | UI flow for register + login |
| Class + material workflow UI | 3–10 | Full workflow in the GUI |
| Review dialog UI | 7, 8 | Usability of review screens |
| API response times | All | Speed under load |
| Big file upload/download | 4, 6 | Large files don't break things |
| Concurrent users | All | Multiple users at the same time |

## Test Cases

### Functional Tests

**Register a new user**

- **Setup:** DB is running, no user "testuser1" or "test1@example.com" exists yet.
- **Steps:** POST to `/api/users/register` with `{ "username": "testuser1", "email": "test1@example.com", "password": "SecurePass123" }`
- **Expected:** 201 response. User shows up in the `users` table. The stored password is a BCrypt hash, not the plaintext.

**Register with taken username/email**

- **Setup:** "testuser1" / "test1@example.com" already registered.
- **Steps:** POST same register endpoint with the same username+email again.
- **Expected:** 400 or 409 error. No duplicate gets created in the DB.

**Login**

- **Setup:** "testuser1" exists with known password.
- **Steps:** POST `/api/users/login` with `{ "email": "test1@example.com", "password": "SecurePass123" }`
- **Expected:** 200 with a JWT token in the response. Token should contain the user's email in the claims.

**Login with wrong password**

- **Setup:** "testuser1" exists.
- **Steps:** POST login with `"password": "WrongPassword"`.
- **Expected:** 401 error. No token returned.

**Create a class**

- **Setup:** testuser1 is logged in (has valid JWT).
- **Steps:** POST `/api/courses` with JWT in header, body: `{ "className": "Math 101", "topic": "Mathematics" }`
- **Expected:** 201. New row in `classes` table with testuser1 as `creator_id`. Also a row in `participants` since the creator gets auto-enrolled.
- **EXTRA might need to store the class id so other user can test joining class.

**Create class with a name that's already used**

- **Setup:** "Math 101" class exists.
- **Steps:** POST `/api/courses` trying to create another "Math 101".
- **Expected:** 400 or 409. DB constraint on `class_name` should prevent it.

**Upload a file to a class**

- **Setup:** testuser1 is the creator of "Math 101".
- **Steps:** POST `/api/materials/upload` with JWT, multipart form with a file like "calculus_notes.pdf", the `classId`, and `materialType` set to something like "Study Guide".
- **Expected:** 201. File appears in `uploads/` folder with a UUID-based name. Row in `materials` table has the original filename, stored filename, path, type, class ID, user ID.

**Non-creator tries to upload**

- **Setup:** testuser2 is logged in but didn't create "Math 101".
- **Steps:** POST upload endpoint with testuser2's JWT, targeting Math 101.
- **Expected:** 403. No file gets saved.

**Join a class**

- **Setup:** testuser2 is logged in. Math 101 exists. testuser2 isn't enrolled yet.
- **Steps:** POST `/api/participants` with `{ "userId": <testuser2_id>, "classId": <math101_id> }`
- **Expected:** 200/201. New row in `participants` linking testuser2 to the class.
- **EXTRA use the id gotten from create class test

**Download material as enrolled user**

- **Setup:** testuser2 is enrolled in Math 101. "calculus_notes.pdf" was uploaded to the class.
- **Steps:** GET `/api/materials/download/{fileId}` with testuser2's JWT.
- **Expected:** 200, file content comes back with the right Content-Type header. The downloaded file should match what was uploaded.

**Download when not enrolled**

- **Setup:** testuser3 exists and is logged in, but is NOT in Math 101.
- **Steps:** GET the same download endpoint with testuser3's JWT.
- **Expected:** 403 — access denied.

**Leave a review**

- **Setup:** testuser2 is enrolled in Math 101. There's a material with a known fileId.
- **Steps:** POST `/api/reviews` with `{ "review": "Very helpful notes!", "rating": 4, "fileId": <id>, "userId": <testuser2_id> }`
- **Expected:** 201. New row in `reviews` table with the text, rating=4, and correct foreign keys. `created_at` should be auto-filled.

**Review with invalid rating**

- **Setup:** testuser2 is enrolled and logged in.
- **Steps:** POST `/api/reviews` but set `"rating": 6` (above the 0–5 range).
- **Expected:** 400 error. The DB has a CHECK constraint on this anyway (rating between 0 and 5), so no record should be created.

**View reviews**

- **Setup:** At least one review exists for a material.
- **Steps:** GET `/api/reviews/material/{fileId}` with testuser1's JWT.
- **Expected:** 200 with a JSON array of reviews — each one should have the reviewer info, review text, rating, and timestamp.

**Delete a material**

- **Setup:** testuser1 is creator of Math 101. A material exists.
- **Steps:** DELETE `/api/materials/{fileId}` with testuser1's JWT.
- **Expected:** 200/204. Row removed from `materials`. The actual file in `uploads/` should be deleted too. Any reviews for that material should also be gone (FK cascade).

**Delete a class**

- **Setup:** testuser1 created Math 101. The class has participants, materials, and reviews.
- **Steps:** DELETE `/api/courses/{classId}` with testuser1's JWT.
- **Expected:** 200/204. Class row gone. Everything linked to it — participants, materials, reviews — gets cascade-deleted. Files on disk should be cleaned up too.

**Logout**

- **Setup:** testuser1 is logged in via the JavaFX client.
- **Steps:** Click "Log Out" in the UI, then try to open a protected view like the class dashboard.
- **Expected:** Taken back to the login screen. The JWT in SessionManager is cleared. Protected endpoints won't work anymore from the client.

### Usability Tests

**Registration & login flow**

- **Setup:** App is running, tester has no account.
- **Steps:**
  1. Open the app
  2. Go to registration screen
  3. Fill in username, email, password
  4. Submit
  5. Go to login
  6. Enter the credentials
  7. Log in
- **What we're checking:** Labels on the forms are clear. If you leave a field empty or put a bad email format, you get feedback right away. After registering you know to go login. Successful login takes you to the home dashboard. The language selector works on both screens (we have i18n support).

**Creating a class, uploading, downloading, deleting**

- **Setup:** User is logged in.
- **Steps:**
  1. Create a class from dashboard
  2. Open the class
  3. Upload a file with a category picked
  4. Check the material shows up in the list
  5. (As another user) join the class and download the file
  6. (As creator) delete the material
  7. Delete the class
- **What we're checking:** Can you do all of this without getting lost? Category selection should be a dropdown or something obvious. Upload should give some kind of confirmation. Material list should show the filename, type, date. Deleting should ask "are you sure?" first.

**Leaving and reading reviews**

- **Setup:** User is enrolled in a class with materials.
- **Steps:**
  1. Pick a material
  2. Open review dialog
  3. Write something, pick a rating
  4. Submit
  5. Open "View Reviews" for same material
- **What we're checking:** The review dialog makes sense — there's a text field and a way to pick 0–5 rating. After submitting, your review should show up in the list with your name, the text, rating, and date. If there are a lot of reviews the list should scroll.

### Performance / Reliability Tests

**Response times**

- **Setup:** Database has some data in it (around 50 users, 20 classes, 100 materials).
- **Steps:** Fire off 100 requests to each of the main endpoints (login, list courses, list materials, get reviews) one after another. Measure average time.
- **Expected:** It Should not crash

**Big file handling**

- **Setup:** Logged in as a class creator.
- **Steps:** Upload a 50 MB file. Download it. Compare checksums.
- **Expected:** Both upload and download finish without timing out. Checksums match — file wasn't corrupted. Server doesn't run out of memory.

**Multiple users at once**

- **Setup:** 10 user accounts in the system.
- **Steps:** Simulate all 10 logging in and doing stuff at the same time — browsing classes, downloading files, posting reviews.
- **Expected:** Nobody gets errors or corrupted data. Each user only sees what they should see (their own classes etc.). DB constraints hold up — no duplicate enrollments or orphaned rows.

## Test Environment

We're running tests against this setup:
- **App:** JavaFX desktop client talking to the Javalin REST backend
- **DB:** MariaDB (database name: `otptestdb`)
- **Auth:** JWT via JJWT 0.12.6, passwords hashed with BCrypt 0.10.2
- **Build:** Maven, Java 21
- **CI:** Jenkins pipeline — runs tests and generates JaCoCo coverage reports

## When to Start / When We're Done

**Before we start testing:**
- App builds with `mvn clean package` without errors
- DB is set up with the schema from `init.sql`
- Existing unit tests (DAO tests + integration test) all pass
- Server starts up and responds

**We're done when:**
- All the functional tests pass

## Possible Issues

| What could go wrong | How we deal with it |
|---------------------|---------------------|
| DB not running when we try to test | Start MariaDB before the test run |
| DB filled with results from previous tests | Initialize the db again or delete all data |
| File permission issues in `uploads/` | Check permissions on the directory before starting |
| JWT tokens expiring mid-test | Generate a fresh token for each test case, don't reuse |
| Cascade delete not working properly | Already have ON DELETE CASCADE in the schema, but we verify this in the "delete class" test |
| Weird stuff with concurrent DB access | That's what the concurrent users test is for — make sure transaction isolation is set right |
