## Overview of Test Cases

- **Test Case 1:** Prevents creation of classes with duplicate names.
- **Test Case 2:** Ensures non-creators cannot upload materials.
- **Test Case 3:** Restricts downloading materials to enrolled users only.
- **Test Case 4:** Allows enrolled users to download materials.
- **Test Cases 5-7:** Cover leaving reviews, validating review input, and viewing reviews.
- **Test Cases 8-9:** Deal with deleting materials and classes by creators.
- **Test Case 10:** Ensures logout revokes access to protected views.

---

## Test Results Summary Table

| Test Case | Description | Arttu | Elias | Aleksi | Eero |
|------------|--------------|--------|--------|---------|-------|
| 1 | User cannot create a class with a duplicate name | Pass | Pass | Pass | Pass |
| 2 | Non-creator cannot upload material | Pass | Pass | Pass | Pass |
| 3 | User cannot download material when not enrolled | Pass | Pass | Pass | Pass |
| 4 | User can download material when enrolled | Fail | Pass | Pass| Pass |
| 5 | User can leave a valid review | Pass | Pass | Pass | Pass |
| 6 | User cannot leave a review with invalid rating | Pass | Pass | Pass | Pass |
| 7 | User can view all reviews | Pass | Pass | Pass | Pass |
| 8 | Class creator can delete a material | Pass | Pass | Pass | Pass |
| 9 | Class creator can delete a class (cascade delete) | Pass | Pass | Pass | Pass |
| 10 | Logout removes access to protected views | Pass | Pass | Pass | Pass |