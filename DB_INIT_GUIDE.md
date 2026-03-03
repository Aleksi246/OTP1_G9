# Database Initialization Guide

## Problem
Previously, the app ran **destructive** database initialization on every startup (`DROP DATABASE`), which is **safe for development but dangerous in production** (loses all data).

## Solution
Now using **environment-aware initialization** with two SQL strategies:

### 1. **Production-Safe: `init.sql` (DEFAULT)**
- Uses `CREATE TABLE IF NOT EXISTS` 
- **Does NOT drop** existing data
- **Idempotent**: Safe to run every startup, production-ready
- Runs by default when app starts
- ✅ Recommended for production and CI/CD pipelines

### 2. **Development-Only: `init-dev.sql` (OPTIONAL)**
- Contains `DROP DATABASE IF EXISTS` 
- **Destroys and recreates** all data from scratch
- ⚠️ Use ONLY in development/testing
- Activated via environment variable

---

## How to Use

### Default Behavior (Safe for Production)
```bash
# Just run the app normally - uses init.sql
java -jar app.jar
mvn spring-boot:run  # or your build command
```
✅ Idempotent, preserves data, safe to restart anytime

---

### Development: Fresh Start (Destructive Reset)
Set the environment variable before running:

**On macOS/Linux:**
```bash
export APP_RESET_DB=true
java -jar app.jar
```

**Or inline:**
```bash
APP_RESET_DB=true java -jar app.jar
```

**In IDE (IntelliJ IDEA):**
1. Run → Edit Configurations
2. Add to "Environment variables": `APP_RESET_DB=true`
3. Click OK and run

This will use `init-dev.sql` (destructive) instead of `init.sql`.

---

### Custom SQL File (Advanced)
You can also specify a custom SQL file:
```bash
APP_INIT_FILE=/path/to/custom.sql java -jar app.jar
```

---

## Code Details

**`DatabaseInitializer.java`** now checks:
- `APP_RESET_DB` environment variable (default: false)
- If true → uses `init-dev.sql` (destructive)
- If false → uses `init.sql` (idempotent)

**Key Methods:**
- `initializeDatabase()`: Smart init with env var support
- `initializeWithFile(String path)`: Init with custom SQL file

---

## Recommended Usage

| Environment | Variable | File | Behavior |
|---|---|---|---|
| **Production** | (unset) | `init.sql` | Create schema only, preserve data ✅ |
| **Development** | `APP_RESET_DB=true` | `init-dev.sql` | Fresh start every time 🔄 |
| **CI/CD Testing** | (unset) | `init.sql` | Reproducible without data loss ✅ |
| **Custom** | `APP_INIT_FILE=/path` | Custom | Use any SQL file 🔧 |

---

## Migration Notes

**Before this change:**
- ❌ Destructive reset on every startup
- ❌ Not safe for production
- ❌ Lost all data when app restarted

**After this change:**
- ✅ Safe idempotent init by default
- ✅ Production-ready
- ✅ Optional destructive reset for dev

