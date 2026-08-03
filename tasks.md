# Implementation Plan: Lock Page Restriction Extension

## Status: Phase 1 — Not Started

## Tasks

### Phase 1: Core Lock Feature

- [ ] 1. Create application space and backend handler
  - [ ] 1.1 Create `XWikiLock` space with WebHome
  - [ ] 1.2 Create `XWikiLock.Code.RestrictionHandler` (Groovy page)
    - Accepts: page reference, restriction mode, list of users/groups with levels
    - Validates caller has permission to set restrictions
    - Writes/removes XWikiRights objects
    - Returns JSON result
  - [ ] 1.3 Create `XWikiLock.Code.RestrictionCheck` (Velocity/Groovy)
    - Fast check: does a page have explicit XWikiRights?
    - Returns: restriction level (none, edit, full)
    - Used by the lock icon to determine state

- [ ] 2. Build the Lock Icon (JSX)
  - [ ] 2.1 Create JSX on `XWikiLock.Code.LockStyleSheet`
    - Injects lock icon into page header (#document-title area or .xcontent header)
    - Checks restriction state via AJAX call to RestrictionCheck
    - Sets icon color based on state (gray/orange/red)
    - Only shows for users with edit+ rights
  - [ ] 2.2 Handle click → open popup

- [ ] 3. Build the Restriction Popup
  - [ ] 3.1 HTML/CSS for modal overlay
    - Mode selector (3 options)
    - User/group search input with autocomplete
    - Permissions list (added users/groups with level dropdown + remove)
    - Apply / Cancel buttons
  - [ ] 3.2 JavaScript logic
    - Load current restrictions via AJAX
    - Populate existing entries
    - Autocomplete search (reuse Crowd pattern)
    - Add/remove entries
    - Apply → POST to RestrictionHandler
    - Update lock icon state without page refresh
  - [ ] 3.3 CSS styling (match xWiki theme, accessible)

- [ ] 4. Permission Write Logic (RestrictionHandler)
  - [ ] 4.1 "No restrictions" mode
    - Remove all XWikiRights objects from the page
  - [ ] 4.2 "Editing restricted" mode
    - Write XWikiRights: allow edit only for specified subjects
    - View remains inherited (no view restriction)
  - [ ] 4.3 "Viewing and editing restricted" mode
    - Write XWikiRights: allow view+edit only for specified subjects
    - Everyone else implicitly denied
  - [ ] 4.4 Always include page author + superadmin in allowed list
  - [ ] 4.5 Space-level: if page is WebHome, write XWikiGlobalRights on WebPreferences

- [ ] 5. Visual Indicators
  - [ ] 5.1 Restriction banner below page header when page is restricted
  - [ ] 5.2 "Edit restrictions" link for authorized users
  - [ ] 5.3 Lock icon tooltip showing current state

- [ ] 6. Testing
  - [ ] 6.1 Test all 3 modes on a regular page
  - [ ] 6.2 Test inheritance (restrict space → verify child pages blocked)
  - [ ] 6.3 Test access denied for unauthorized users
  - [ ] 6.4 Test that restrictions show in xWiki Crowd Access Matrix
  - [ ] 6.5 Test cannot lock yourself out
  - [ ] 6.6 Test as non-admin user (can only restrict pages they own)

### Phase 2: Crowd Integration (Future)

- [ ] 7. Crowd Dashboard widget showing restricted pages count
- [ ] 8. Crowd Access Matrix shows lock restrictions
- [ ] 9. Audit log entries for lock/unlock events
- [ ] 10. Bulk unlock from Crowd admin
- [ ] 11. "Locked Pages" report in Crowd

## Effort Estimate

| Task | Effort |
|------|--------|
| Backend handler (Groovy) | ~1 hour |
| Lock icon injection (JSX) | ~1 hour |
| Popup UI (HTML/CSS/JS) | ~2 hours |
| Permission write logic | ~1 hour |
| Visual indicators | ~30 min |
| Testing | ~1 hour |
| **Total Phase 1** | **~6-7 hours** |

## Technical Notes

- The JSX must be deployed with `use=always` to appear on ALL pages (not just XWikiLock space)
- The Groovy handler needs Programming Rights (authored by superadmin)
- Lock icon injection point: look for `#document-title` or `#xwikicontent` header area
- For the popup: no external libraries, vanilla JS + CSS
- The autocomplete pattern is identical to xWiki Crowd's — can share the code
