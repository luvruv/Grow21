// OBSOLETE — kept only so existing references don't break.
//
// This script used to add Username/Password columns to the Child table.
// Those columns were removed during normalization (see Grow21_Normalization.sql,
// Step 1) because child auth credentials now live in the centralized User table.
//
// Running this script against a normalized database is a no-op.
console.log('addColumns.js is obsolete — child credentials now live in the User table.');
process.exit(0);
