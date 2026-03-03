# Quick Start Guide - Admin Registration Fix

## TL;DR

Your admin registration was failing because **MariaDB wasn't being initialized**. 

We've fixed it by:
1. ✅ Creating `DatabaseInitializer.java` to automatically set up the database
2. ✅ Adding logging to show you exactly what's happening
3. ✅ Updating Main.java to initialize the database on startup

## What You Need To Do NOW

### Step 1: Start MariaDB
```bash
brew services start mariadb
```

### Step 2: Rebuild the Project
```bash
cd /Users/eliaseide/IdeaProjects/OTP1_G9
mvn clean install
```

### Step 3: Run the App
```bash
mvn exec:java -Dexec.mainClass="com.example.app.Main"
```

### Step 4: Watch the Console
You should see messages like:
```
Executed: DROP DATABASE IF EXISTS otptestdb;...
Executed: CREATE DATABASE IF NOT EXISTS otptestdb;...
Executed: CREATE USER IF NOT EXISTS 'otptestuser'@'localhost'...
...
Database initialization completed successfully!
Server started on port 7700
```

### Step 5: Try Registering Admin
Use REST Client (VS Code) to send:
```http
POST http://localhost:7700/api/auth/register
Content-Type: application/json

{
  "username": "admin1",
  "password": "admin123",
  "userType": "admin",
  "adminKey": "supersecretkey"
}
```

You should see in console:
```
[REGISTRATION] Received registration request
[REGISTRATION] Username: admin1, UserType: admin
[REGISTRATION] Checking admin key
[REGISTRATION] Creating user in database: admin1
[REGISTRATION] User created successfully with ID: 1
```

And get response:
```json
{
  "userId": 1,
  "username": "admin1",
  "userType": "admin",
  "createdAt": "2026-03-03T16:52:13"
}
```

## Files Changed
- ✅ `Main.java` - Added database initialization
- ✅ `UserController.java` - Added registration logging
- ✅ `DatabaseInitializer.java` - NEW file for auto setup

## Files Created (Reference)
- `ADMIN_REGISTRATION_FIX.md` - Detailed setup guide
- `CHANGES_SUMMARY.md` - Technical summary of changes
- `QUICK_START.md` - This file

## If Something Goes Wrong

Check these in order:
1. Is MariaDB running? → `brew services list | grep mariadb`
2. Can you connect to MariaDB? → `mysql -u root`
3. Check console logs for specific error messages
4. See `ADMIN_REGISTRATION_FIX.md` for troubleshooting

## Questions?

The logging will tell you exactly what's happening:
- `[REGISTRATION]` messages = normal progress
- `Exception: ...` = something went wrong (check the error)

That's it! Try running it now! 🚀

