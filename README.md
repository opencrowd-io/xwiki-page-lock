# xWiki Page Lock — Confluence-Style Page Restrictions

A page restriction extension for xWiki. Adds a lock icon to every page, allowing authors and admins to restrict access with one click — like Confluence.

## Features

- 🔓 Lock icon on every page (black = unlocked, red = locked)
- Confluence-style restriction popup with colored mode selector
- 3 restriction modes: No restrictions, Editing restricted, Viewing and editing restricted
- **Autocomplete search** — type 2+ characters to find users/groups
- Per-subject permission level (Can view / Can view and edit)
- Manages both page-level AND space-level restrictions
- Writes native XWiki.XWikiRights — fully enforced by xWiki's permission engine
- Inherited restriction warning (shows when restrictions come from parent space)
- Toast notification on apply
- Hidden on Global Administration pages
- Compatible with xWiki 15.x+

---

## Installation (Step by Step)

### Prerequisites
- xWiki 15.x or later
- Admin/superadmin access (needed for Programming Rights)

### Option A: Automated Deployment (Recommended)

If you have Python + `requests` library installed:

```bash
cd xwiki-page-lock
pip install requests  # if not installed

# Deploy all components
python3 fix_groovy.py    # Deploys Groovy handlers
python3 deploy_jsx.py    # Deploys JavaScript extension
```

Edit `fix_groovy.py` and `deploy_jsx.py` to set your xWiki URL and credentials.

### Option B: Manual Deployment

#### Step 1: Create the RestrictionHandler page

1. Go to your xWiki instance
2. Create a new page: **XWikiLock > Code > RestrictionHandler**
   - URL: `https://your-xwiki.com/bin/create/XWikiLock/Code/RestrictionHandler`
3. Switch to **Wiki editor** (not WYSIWYG)
4. Set syntax to **xwiki/2.1**
5. Paste this content:
   ```
   {{groovy}}
   [paste content of src/RestrictionHandler.groovy here]
   {{/groovy}}
   ```
6. Save the page (you must be logged in as admin/superadmin)

#### Step 2: Create the RestrictionCheck page

1. Create page: **XWikiLock > Code > RestrictionCheck**
2. Switch to Wiki editor, syntax: xwiki/2.1
3. Paste:
   ```
   {{groovy}}
   [paste content of src/RestrictionCheck.groovy here]
   {{/groovy}}
   ```
4. Save

#### Step 3: Create the JavaScript Extension (LockIconJSX)

1. Create page: **XWikiLock > Code > LockIconJSX**
2. Edit the page
3. At the bottom, click **"Objects"** (or go to Object Editor)
4. Add a new object of class: **XWiki.JavaScriptExtension**
5. Fill in the object fields:
   - **code**: Paste the entire content of `src/LockIcon.js`
   - **use**: `always` (this makes it load on every page)
   - **cache**: `long`
   - **parse**: `0` (unchecked)
6. Save

#### Step 4: Create the CSS Extension (LockStyleSSX)

1. Create page: **XWikiLock > Code > LockStyleSSX**
2. Add object: **XWiki.StyleSheetExtension**
3. Fields:
   - **code**: Paste content of `src/LockStyle.css`
   - **use**: `always`
   - **cache**: `long`
   - **contentType**: `CSS`
4. Save

#### Step 5: Verify

1. Navigate to any wiki page
2. You should see a small padlock icon (🔓 black) next to "Last modified by..."
3. Click it → the Restrictions popup opens
4. Set "Editing restricted" → type a username (autocomplete appears) → Apply
5. Verify: the lock turns red, other users can't edit

---

## How It Works

```
User clicks lock icon
      │
      ▼
Popup opens (Confluence-style)
  - Choose mode (No restrictions / Editing / Full)
  - Search users/groups (autocomplete)
  - Add with permission level
  - Apply
      │
      ▼
RestrictionHandler.groovy
  - Validates permissions
  - Writes XWiki.XWikiRights (page) or XWiki.XWikiGlobalRights (space)
  - Saves document
      │
      ▼
xWiki Permission Engine enforces immediately
```

---

## Restriction Modes

| Mode | Icon | Effect |
|------|------|--------|
| No restrictions | 🔓 Black (unlocked) | Everyone can view and edit |
| Editing restricted | 🔒 Red (locked) | Everyone can view, only specified users can edit |
| Viewing and editing restricted | 🔒 Red (locked) | Only specified users can view or edit |

---

## Space-Level vs Page-Level Restrictions

- **Page-level**: Restriction applies only to that specific page
- **Space-level**: When you restrict a space's WebHome page, it writes to WebPreferences and all child pages inherit the restriction
- When removing restrictions, the extension clears BOTH levels

---

## Files

| File | Purpose |
|------|---------|
| `src/LockIcon.js` | Frontend: lock icon, popup UI, autocomplete, apply logic |
| `src/LockStyle.css` | CSS styling for restriction banner |
| `src/RestrictionHandler.groovy` | Backend: validates permissions, writes XWikiRights |
| `src/RestrictionCheck.groovy` | Backend: checks current restriction state (page + space + parent) |
| `deploy_jsx.py` | Automated deployment script for JavaScript extension |
| `fix_groovy.py` | Automated deployment script for Groovy handlers |

---

## Configuration

The deploy scripts use these defaults (edit to match your instance):

```python
base = 'https://your-xwiki.com'
auth = ('admin_username', 'admin_password')
```

---

## Troubleshooting

**Lock icon not showing:**
- Check that the JSX object has `use=always`
- Hard-refresh the page (Cmd+Shift+R)
- Check browser console for JavaScript errors

**"Failed to apply restrictions" error:**
- The Groovy handlers need Programming Rights
- Make sure RestrictionHandler/RestrictionCheck pages are saved by an admin user

**Lock bounces back to red after removing:**
- Pre-existing space-level restrictions (set through xWiki admin) need to be removed from Administration → Rights
- The extension shows a warning when restrictions are inherited

**Autocomplete not showing results:**
- Need at least 2 characters to trigger search
- Searches the XWiki space (where users and groups live)

---

## Compatibility

- xWiki 15.x, 16.x, 17.x+
- Works with FlamingoTheme (default skin)
- Compatible with OpenCrowd Access Matrix
- No external libraries required

---

## License

Apache-2.0

---

## Built By

[OpenCrowd](https://opencrowd.io) — Open Identity & Access Governance.

Page Lock integrates with OpenCrowd's Access Matrix for centralized permission visibility across xWiki, OpenProject, and Nextcloud.
