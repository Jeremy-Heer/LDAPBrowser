# ACI Builder Dialog LDIF Copy Removal Summary

## Changes Made

Successfully removed the "Copy as LDIF" functionality from the AciBuilderDialog as requested. The LDIF copy feature is now only available in the Add/Edit ACI dialogs where the target DN is always available.

## Specific Changes

### AciBuilderDialog.java

**Removed:**
1. `copyLdifButton` field declaration
2. Copy button initialization and configuration in the footer
3. `copyAciAsLdif()` method
4. `generateAddLdif()` method
5. References to `copyLdifButton` in the `updatePreview()` method
6. Unused `UI` import

**Retained:**
- All core ACI building functionality
- Original footer with just "Cancel" and "Build ACI" buttons
- All validation and preview functionality

## Current State

- **AciBuilderDialog**: No LDIF copy functionality - focuses purely on ACI construction
- **AddAciDialog**: Retains full LDIF copy functionality with proper DN context
  - Add mode: Generates LDIF to add new ACI to specific entry
  - Edit mode: Generates LDIF to update existing ACI (delete + add)

## Benefits of This Change

1. **Simplified Workflow**: AciBuilderDialog now has a cleaner, more focused interface
2. **Better UX**: LDIF generation only happens when the target DN is known and specified
3. **Consistency**: All LDIF operations now include the actual target DN rather than placeholders
4. **Code Clarity**: Removed duplicate LDIF generation logic

The implementation ensures that users can still create ACIs using the visual builder and then copy them as LDIF from the Add/Edit dialogs where the target DN context is available.