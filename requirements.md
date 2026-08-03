# Requirements: Lock Page Restriction Extension

## Overview

A Confluence-inspired page restriction extension for xWiki that adds an inline "Lock" icon to every page header, allowing page authors and admins to quickly restrict access directly from the page view — without navigating to admin panels.

## Core Objective

Enable any page author (or admin) to set access restrictions on a page or space directly from the page's header bar, using a simple popup interface with three restriction levels. All restrictions write to native `XWiki.XWikiRights` / `XWiki.XWikiGlobalRights` objects, ensuring full compatibility with xWiki's permission system and xWiki Crowd.

## Phases

| Phase | Scope |
|-------|-------|
| Phase 1 | Lock icon + popup + 3 restriction modes + user/group search + inheritance to children |
| Phase 2 | Integration with xWiki Crowd (dashboard widget showing locked pages, audit trail, bulk lock/unlock from Crowd) |

## Functional Requirements

### REQ-1: Lock Icon in Page Header

1. The extension SHALL display a lock icon (🔒) in the page header bar of every wiki page
2. The lock icon SHALL be visible to users who have `edit` or `admin` rights on the current page
3. The lock icon SHALL indicate the current restriction state:
   - 🔓 Open (gray) — No restrictions
   - 🔒 Partial (orange) — Editing restricted
   - 🔒 Full (red) — Viewing and editing restricted
4. Clicking the lock icon SHALL open a restriction popup/modal

### REQ-2: Restriction Popup

1. The popup SHALL offer three restriction modes:
   - **No restrictions** — Everyone can view and edit (default)
   - **Editing restricted** — Everyone can view, only specified users/groups can edit
   - **Viewing and editing restricted** — Only specified users/groups can view or edit
2. The popup SHALL display:
   - Current restriction mode (dropdown/selector)
   - List of currently permitted users/groups with their access level
   - A search field to add users or groups (with autocomplete)
   - Access level selector per user/group: "Can view" or "Can view and edit"
   - Remove button per entry
   - Apply and Cancel buttons

### REQ-3: User and Group Search

1. The search field SHALL provide autocomplete for users and groups (same as xWiki Crowd's @mention)
2. Search SHALL query native `XWiki.XWikiUsers` and `XWiki.XWikiGroups`
3. Results SHALL display: name, type (User/Group), and an "Add" action
4. Minimum 2 characters before search triggers

### REQ-4: Permission Write-Through

1. When "Apply" is clicked, the extension SHALL write native `XWiki.XWikiRights` objects on the current page:
   - **No restrictions**: Remove all `XWiki.XWikiRights` objects from the page
   - **Editing restricted**: Add `XWiki.XWikiRights` with `levels=edit` + `allow=1` for specified users/groups (implicitly denies edit to everyone else; view remains inherited)
   - **Viewing and editing restricted**: Add `XWiki.XWikiRights` with `levels=view,edit` + `allow=1` for specified users/groups (implicitly denies both view and edit to everyone else)
2. All writes MUST use native `XWiki.XWikiRights` objects so they are enforced by xWiki's permission engine immediately
3. The page author and superadmin SHALL always be included in the allowed list (cannot lock yourself out)

### REQ-5: Inheritance to Child Pages

1. When a restriction is applied to a space's WebHome (top-level page), the extension SHALL write `XWiki.XWikiGlobalRights` on the space's `WebPreferences` instead of page-level rights
2. This SHALL cause all child pages within that space to inherit the restriction
3. The popup SHALL indicate: "This restriction applies to all pages in this space" when set on a space homepage
4. Individual child pages can override with their own page-level restrictions

### REQ-6: Access Control on Lock Feature

1. Only users with `edit` permission on the page SHALL be able to set "Editing restricted"
2. Only users with `admin` permission on the page/space SHALL be able to set "Viewing and editing restricted"
3. Only `XWikiAdminGroup` members or superadmin SHALL be able to modify restrictions set by other users

### REQ-7: Visual Indicators

1. When a page is restricted, the page SHALL display a subtle banner below the header: "This page has restricted access" with the restriction level
2. Users who can modify restrictions SHALL see an "Edit restrictions" link in the banner
3. Users who cannot access a restricted page SHALL see xWiki's standard "Access Denied" message

## Non-Functional Requirements

### NFR-1: Performance
- Lock icon state must render within 100ms (cached check)
- Popup must load within 500ms
- Apply action must complete within 2 seconds

### NFR-2: Compatibility
- Works with xWiki 17.x+
- Compatible with xWiki Crowd (restrictions show in Crowd's Access Matrix)
- Works with any xWiki skin (FlamingoTheme default)
- Does NOT require Programming Rights for end users (only for deployment)

### NFR-3: Deployment
- Deployed as an xWiki extension (.xar) or via Skin Extension (SSX + JSX)
- No server restart required
- Can be enabled/disabled per space via configuration

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────┐
│  Page Header (every page)               │
│  [🔒 Lock Icon]                         │
└──────────┬──────────────────────────────┘
           │ click
           ▼
┌─────────────────────────────────────────┐
│  Restriction Popup (JavaScript modal)   │
│  - Mode selector                        │
│  - User/Group search (autocomplete)     │
│  - Permissions list                     │
│  - Apply / Cancel                       │
└──────────┬──────────────────────────────┘
           │ Apply
           ▼
┌─────────────────────────────────────────┐
│  Groovy REST handler                    │
│  - Validates permissions                │
│  - Writes XWiki.XWikiRights objects     │
│  - Returns updated state                │
└──────────┬──────────────────────────────┘
           │ writes
           ▼
┌─────────────────────────────────────────┐
│  Native xWiki Rights                    │
│  XWiki.XWikiRights on page              │
│  XWiki.XWikiGlobalRights on space       │
│  (Enforced by xWiki permission engine)  │
└─────────────────────────────────────────┘
```

### Implementation Components

| Component | Technology | Location |
|-----------|-----------|----------|
| Lock icon injection | JSX (JavaScript Skin Extension) | Global — checks every page |
| Popup UI | HTML + CSS + JavaScript (no framework) | Injected by JSX |
| Autocomplete | Reuse xWiki Crowd's autocomplete pattern | Shared JSX or inline |
| Backend handler | Groovy page (REST-like) | `XWikiLock.Code.RestrictionHandler` |
| State check | Velocity or AJAX call | Fast check if page has XWikiRights |

### Phase 2: Crowd Integration

- Crowd's Access Matrix shows pages with lock restrictions
- Crowd Dashboard widget: "Restricted Pages" count
- Crowd Audit Log captures lock/unlock events
- Crowd admins can bulk-unlock pages
- Crowd "Spaces" page shows lock status per space

## Acceptance Criteria Summary

| # | Criteria |
|---|----------|
| AC-1 | Lock icon visible on all pages for users with edit+ rights |
| AC-2 | Three restriction modes work correctly |
| AC-3 | User/group autocomplete search works in popup |
| AC-4 | Apply writes native XWikiRights objects |
| AC-5 | Permissions are enforced immediately after Apply |
| AC-6 | Space-level restrictions inherit to child pages |
| AC-7 | Cannot lock yourself out (author always included) |
| AC-8 | Visual indicator shows restriction status |
| AC-9 | Works without page refresh (AJAX) |
| AC-10 | Compatible with xWiki Crowd Access Matrix |
