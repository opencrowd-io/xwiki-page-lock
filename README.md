# xWiki Page Lock — Restriction Extension

A Confluence-inspired page restriction extension for xWiki. Adds a lock icon to every page header, allowing page authors and admins to restrict access with one click.

## Features

- 🔓 Lock icon on every page header (visible to editors/admins)
- 3 restriction modes: No restrictions, Editing restricted, Viewing and editing restricted
- User/group search with autocomplete
- Writes native XWiki.XWikiRights — fully enforced by xWiki's permission engine
- Space-level inheritance (restrict a space homepage → all children inherit)
- Cannot lock yourself out (author + admin always included)
- No page refresh needed (AJAX)
- Compatible with xWiki 15.x+

## Installation

### Step 1: Create the XWikiLock space

In xWiki, create these pages:

| Page | Content | Content Type |
|------|---------|-------------|
| `XWikiLock.Code.RestrictionHandler` | Copy from `src/RestrictionHandler.groovy` | `plain/text` |
| `XWikiLock.Code.RestrictionCheck` | Copy from `src/RestrictionCheck.groovy` | `plain/text` |
| `XWikiLock.Code.LockIconJSX` | Copy from `src/LockIcon.js` | JavaScript Skin Extension |
| `XWikiLock.Code.LockStyleSSX` | Copy from `src/LockStyle.css` | CSS Skin Extension |

### Step 2: Configure Skin Extensions

For the JavaScript extension (`LockIconJSX`):
1. Edit the page → add an object of type `XWiki.JavaScriptExtension`
2. Set `use` = `always` (so it loads on every page)
3. Set `cache` = `long`
4. Paste the content of `src/LockIcon.js` into the code field

For the CSS extension (`LockStyleSSX`):
1. Edit the page → add an object of type `XWiki.StyleSheetExtension`
2. Set `use` = `always`
3. Paste the content of `src/LockStyle.css` into the code field

### Step 3: Set Programming Rights

The Groovy pages (`RestrictionHandler` and `RestrictionCheck`) must be saved by a user with Programming Rights (typically `XWiki.Admin` or superadmin).

### Step 4: Test

1. Navigate to any wiki page
2. You should see a 🔓 icon in the page header
3. Click it → restriction popup opens
4. Set "Editing restricted" → add a user → Apply
5. Verify: other users can view but not edit the page

## How It Works

```
User clicks 🔒 icon
      │
      ▼
Popup opens (3 modes)
      │
      ▼ Apply
RestrictionHandler.groovy
      │
      ▼ writes
XWiki.XWikiRights objects
      │
      ▼ enforced by
xWiki Permission Engine
```

## Restriction Modes

| Mode | Effect |
|------|--------|
| No restrictions | Everyone can view and edit (default) |
| Editing restricted | Everyone can view, only specified users/groups can edit |
| Viewing and editing restricted | Only specified users/groups can view or edit |

## Space-Level Restrictions

When you restrict a space's WebHome page, the extension automatically writes `XWiki.XWikiGlobalRights` on the space's `WebPreferences`. This causes all child pages in the space to inherit the restriction.

## Compatibility

- xWiki 15.x, 16.x, 17.x+
- Works with FlamingoTheme (default skin)
- Compatible with OpenCrowd Access Matrix
- No external libraries required

## License

Apache-2.0
