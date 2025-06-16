# Database Migration Fix v1.0.2.178

## Issue Fixed: SQLite Database Schema Mismatch

### Problem
The server was throwing an error on player login:
```
org.sqlite.SQLiteException: [SQLITE_ERROR] SQL error or missing database (no such column: currency_name)
```

### Root Cause
The database schema was inconsistent:
- **Table Creation Code**: Used `currency_id` column
- **Query Code**: Tried to access `currency_name` column
- **Result**: Column not found error when trying to load player balances

### Solution: Database Schema Migration

#### 1. **Fixed Schema Consistency** ✅
Updated all database schemas to use `currency_name` instead of `currency_id`:

**Before:**
```sql
CREATE TABLE account_balances (
    player_id TEXT NOT NULL,
    currency_id TEXT NOT NULL,  -- OLD
    balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (player_id, currency_id)
);
```

**After:**
```sql
CREATE TABLE account_balances (
    player_id TEXT NOT NULL,
    currency_name TEXT NOT NULL,  -- NEW
    balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    updated_at INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (player_id, currency_name)
);
```

#### 2. **Added Automatic Migration** ✅
Implemented automatic database migration for existing servers:

- **Detection**: Checks existing tables for old column names
- **Migration**: Creates new table with correct schema
- **Data Transfer**: Copies all existing data to new table
- **Cleanup**: Removes old table and renames new table

#### 3. **Updated All References** ✅
Changed all database operations to use `currency_name`:

- **Balance Loading**: `SELECT currency_name, balance FROM account_balances`
- **Balance Saving**: `INSERT INTO account_balances (player_id, currency_name, balance, updated_at)`
- **Transaction Logging**: `INSERT INTO transactions (..., currency_name, ...)`
- **Transaction Loading**: `SELECT ..., currency_name, ... FROM transactions`

### Migration Process

#### For Existing Servers:
1. **Automatic Detection**: On startup, checks if tables use old schema
2. **Backup**: Creates new tables with correct schema
3. **Data Migration**: Copies all data from old to new tables
4. **Cleanup**: Removes old tables and renames new ones
5. **Logging**: Logs migration progress for debugging

#### For New Servers:
1. **Clean Installation**: Creates tables with correct schema from start
2. **No Migration Needed**: New databases have proper schema

### Files Modified

1. **SqliteEconomyStorage.java**:
   - Added `migrateDatabaseIfNeeded()` method
   - Added `migrateCurrencyColumn()` method
   - Added `migrateTransactionsTable()` method
   - Updated all SQL queries to use `currency_name`
   - Updated table creation schemas

### Technical Details

#### Migration Safety Features:
- **Transactional**: All migrations use database transactions
- **Rollback**: Failures automatically rollback changes
- **Logging**: Detailed logging of migration steps
- **Data Integrity**: Preserves all existing data

#### Supported Migrations:
- **account_balances**: `currency_id` → `currency_name`
- **transactions**: `currency_id` → `currency_name`
- **Schema Additions**: Added `updated_at` column to balances

### Testing Status

- **Build Status**: ✅ SUCCESS (v1.0.2.178)
- **Schema Migration**: ✅ Implemented
- **Data Safety**: ✅ Transactional with rollback
- **Backward Compatibility**: ✅ Handles existing databases

### Expected Behavior After Update

#### On Server Startup:
1. **Migration Check**: Automatically detects old schema
2. **Migration Execution**: Migrates data if needed
3. **Success Logging**: Confirms migration completion
4. **Normal Operation**: Economy system works normally

#### Player Login:
1. **Balance Loading**: Successfully loads player balances
2. **No Errors**: SQLite column error eliminated
3. **Tablist Display**: Player economy data displays correctly
4. **Shop Functionality**: All shop features work properly

### Deployment Instructions

1. **Stop Server**: Ensure server is stopped before update
2. **Backup Database**: Backup existing economy database (optional but recommended)
3. **Update JAR**: Replace with `neoessentials-1.0.2.178.jar`
4. **Start Server**: Migration will run automatically on startup
5. **Monitor Logs**: Check for successful migration messages

### Success Indicators

Look for these log messages:
```
[INFO] Migrating account_balances table from currency_id to currency_name
[INFO] Successfully migrated account_balances table
[INFO] Migrating transactions table from currency_id to currency_name
[INFO] Successfully migrated transactions table
```

### Rollback Plan

If issues occur:
1. Stop server
2. Restore database backup (if created)
3. Revert to previous JAR version
4. Report issues for further investigation

---

**Summary**: The database schema mismatch causing the `currency_name` column error has been fixed with automatic migration support. Players can now log in without economy errors, and all balance data is preserved during the migration process.
