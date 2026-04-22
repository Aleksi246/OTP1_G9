# Bug and Issues Tracker — OTP1_G9

| ID | Title | Description | Severity | Status | Component | Resolution | Notes |
|---|---|---|---|---|---|---|---|
| BUG-001 | Responsive layout issue - window resizes on button click | When pressing buttons (Create Class, Upload, Join Class, etc.), the UI window suddenly changes size/shifts layout. Affects user experience and causes content repositioning. Steps: 1) Open application on JavaFX window 2) Navigate to button (Create Class button) 3) Click button 4) Window jumps/resizes unexpectedly | Low | Open | UI/UX | | Likely caused by layout managers not properly constraining component sizes or event handlers triggering layout refresh cycles |
| BUG-002 | Jenkins build fails - db.properties and .env files missing | Jenkins CI/CD pipeline fails during mvn test and sonar:sonar stages because db.properties and .env files are not in git repository (removed for security reasons). Error: FileNotFoundException when loading database configuration. Solution: Store credentials in Jenkins Secrets, use environment variables, or Maven profiles with environment variable substitution. Steps: 1) Run build on Jenkins 2) Build fails at Database connection stage 3) Check logs - file not found error | Critical | Resolved | Infrastructure | Create Jenkins secrets store; configure Maven to use environment variables; document CI/CD setup | Security best practice (credentials not in VCS), but breaks local Jenkins integration. Needs Jenkins credential management setup |
| BUG-003 | | | | Open | | | |
| BUG-004 | | | | Open | | | |
| BUG-005 | | | | Open | | | |
| BUG-006 | | | | Open | | | |
| BUG-007 | | | | Open | | | |
| BUG-008 | | | | Open | | | |
| BUG-009 | | | | Open | | | |
| BUG-010 | | | | Open | | | |

**Severity:** Critical | High | Medium | Low  
**Status:** Open | In Progress | In Testing | Resolved | Closed | Deferred | Invalid  
**Component:** Auth | Class Mgmt | Materials | Reviews | Security | Performance | UI/UX | Database | API | Infrastructure
