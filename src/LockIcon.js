/**
 * XWikiLock Page Lock Extension — Confluence-style restrictions
 */
(function() {
    'use strict';

    var CHECK_URL = '/bin/view/XWikiLock/Code/RestrictionCheck?xpage=plain&outputSyntax=plain';
    var HANDLER_URL = '/bin/view/XWikiLock/Code/RestrictionHandler?xpage=plain&outputSyntax=plain';

    var pageRef = (typeof XWiki !== 'undefined' && XWiki.currentDocument)
        ? XWiki.Model.serialize(XWiki.currentDocument.documentReference) : '';
    var currentLevel = 'none';
    var currentSubjects = [];

    // --- Icon in page header (like Confluence's padlock next to breadcrumb) ---
    function createIcon() {
        var btn = document.createElement('button');
        btn.id = 'xwiki-lock-icon';
        btn.style.cssText = 'background:none;border:none;cursor:pointer;padding:2px 6px;font-size:16px;vertical-align:middle;margin-left:8px;transition:all 0.2s;';
        setIconState(btn, 'none');
        btn.onclick = openPopup;
        return btn;
    }

    function setIconState(btn, level) {
        if (level === 'none') {
            // Unlocked — black solid padlock with open shackle (like the image)
            btn.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="#333" xmlns="http://www.w3.org/2000/svg"><rect x="3" y="12" width="18" height="10" rx="2"/><path d="M7 12V8a5 5 0 0 1 9.9-1" stroke="#333" stroke-width="2.5" fill="none" stroke-linecap="round"/><circle cx="12" cy="17" r="1.5" fill="#fff"/></svg>';
            btn.title = 'No restrictions — click to restrict';
        } else {
            // Locked — red solid padlock with closed shackle
            btn.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="#dc2626" xmlns="http://www.w3.org/2000/svg"><rect x="3" y="12" width="18" height="10" rx="2"/><path d="M7 12V8a5 5 0 0 1 10 0v4" stroke="#dc2626" stroke-width="2.5" fill="none" stroke-linecap="round"/><circle cx="12" cy="17" r="1.5" fill="#fff"/></svg>';
            btn.title = level === 'edit' ? 'Editing restricted' : 'Viewing and editing restricted';
        }
    }

    function updateIcon(level) {
        var btn = document.getElementById('xwiki-lock-icon');
        if (!btn) return;
        currentLevel = level;
        setIconState(btn, level);
    }

    function injectIcon() {
        if (document.getElementById('xwiki-lock-icon')) return;
        // Don't show on Global Administration pages
        if (window.location.href.indexOf('/admin/') !== -1 || window.location.href.indexOf('editor=globaladmin') !== -1) return;
        // Inject next to the document metadata (Last modified by...)
        var target = document.querySelector('.xdocLastModification') || document.querySelector('#document-info') || document.querySelector('.doc-info-right');
        if (!target) {
            // Fallback: inject after document title
            target = document.getElementById('document-title') || document.querySelector('#xwikicontent');
        }
        if (!target) return;
        var icon = createIcon();
        target.insertBefore(icon, target.firstChild);
        checkState();
    }

    function checkState() {
        if (!pageRef) return;
        fetch(CHECK_URL + '&page=' + encodeURIComponent(pageRef))
            .then(function(r) { return r.json(); })
            .then(function(data) { currentLevel = data.level || 'none'; currentSubjects = data.subjects || []; updateIcon(currentLevel); })
            .catch(function() { updateIcon('none'); });
    }

    // --- Confluence-style popup ---
    function openPopup() {
        var existing = document.getElementById('xwiki-lock-popup');
        if (existing) { existing.remove(); return; }

        var overlay = document.createElement('div');
        overlay.id = 'xwiki-lock-popup';
        overlay.style.cssText = 'position:fixed;inset:0;z-index:99999;display:flex;align-items:center;justify-content:center;background:rgba(0,0,0,0.4);';

        var modal = document.createElement('div');
        modal.style.cssText = 'background:#fff;border-radius:8px;padding:0;width:520px;max-height:85vh;overflow-y:auto;box-shadow:0 10px 40px rgba(0,0,0,0.25);font-family:-apple-system,BlinkMacSystemFont,sans-serif;';

        // Header
        var header = '<div style="padding:20px 24px 16px;border-bottom:1px solid #eee;">' +
            '<div style="display:flex;align-items:center;justify-content:space-between;">' +
            '<h2 style="margin:0;font-size:20px;font-weight:600;color:#172b4d;">Restrictions</h2>' +
            '<button id="lock-close" style="background:none;border:none;font-size:20px;cursor:pointer;color:#666;">\u2715</button>' +
            '</div></div>';

        // Mode selector (Confluence-style colored dropdown)
        var modeColors = { none: '#0052CC', edit: '#FF991F', full: '#DE350B' };
        var modeIcons = {
            none: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/></svg>',
            edit: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>',
            full: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>'
        };
        var modeLabels = { none: 'No restrictions', edit: 'Editing restricted', full: 'Viewing and editing restricted' };
        var modeDescs = { none: 'Everyone can view and edit', edit: 'Everyone can view, only some can edit', full: 'Only some people can view or edit' };

        var modeSection = '<div style="padding:16px 24px;">' +
            '<div style="display:flex;align-items:center;gap:12px;">' +
            '<select id="lock-mode" style="padding:8px 12px 8px 32px;border-radius:4px;border:none;font-size:14px;font-weight:500;color:#fff;cursor:pointer;background-color:' + modeColors[currentLevel] + ';appearance:none;-webkit-appearance:none;background-image:url(\'data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2212%22 height=%2212%22 viewBox=%220 0 24 24%22 fill=%22white%22><path d=%22M7 10l5 5 5-5z%22/></svg>\');background-repeat:no-repeat;background-position:right 8px center;">' +
            '<option value="none"' + (currentLevel === 'none' ? ' selected' : '') + '>No restrictions</option>' +
            '<option value="edit"' + (currentLevel === 'edit' ? ' selected' : '') + '>Editing restricted</option>' +
            '<option value="full"' + (currentLevel === 'full' ? ' selected' : '') + '>Viewing and editing restricted</option>' +
            '</select>' +
            '<span id="lock-mode-desc" style="font-size:13px;color:#6b778c;">' + modeDescs[currentLevel] + '</span>' +
            '</div></div>';

        // Subjects section
        var subjectsSection = '<div id="lock-subjects-section" style="padding:0 24px 16px;' + (currentLevel === 'none' ? 'display:none;' : '') + '">' +
            '<div style="border-top:1px solid #eee;padding-top:16px;">' +
            '<div style="display:flex;gap:8px;margin-bottom:12px;">' +
            '<input id="lock-search" placeholder="Type a user name or group" style="flex:1;padding:8px 12px;border:2px solid #dfe1e6;border-radius:4px;font-size:14px;box-sizing:border-box;"/>' +
            '<select id="lock-add-level" style="padding:8px 12px;border:2px solid #dfe1e6;border-radius:4px;font-size:13px;background:#fff;">' +
            '<option value="edit">Can view and edit</option>' +
            '<option value="view">Can view</option>' +
            '</select>' +
            '<button id="lock-add-btn" style="padding:8px 16px;border:none;border-radius:4px;background:#0052CC;color:#fff;font-size:13px;font-weight:500;cursor:pointer;">Add</button>' +
            '</div>' +

            '<div id="lock-everyone-row" style="display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid #f4f5f7;' + (currentLevel === 'none' ? '' : '') + '">' +
            '<div style="display:flex;align-items:center;gap:8px;">' +
            '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6b778c" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 21v-1a6 6 0 0 1 12 0v1"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/><path d="M21 21v-1a4 4 0 0 0-3-3.85"/></svg>' +
            '<span style="font-size:14px;color:#172b4d;">Everyone</span></div>' +
            '<span style="font-size:13px;color:#6b778c;">Has no access</span></div>' +

            '<div id="lock-subjects-list" style="margin-top:4px;"></div>' +
            '</div></div>';

        // Footer
        var footer = '<div style="padding:16px 24px;border-top:1px solid #eee;display:flex;justify-content:flex-end;gap:8px;">' +
            '<button id="lock-cancel" style="padding:8px 20px;border:none;border-radius:4px;background:#f4f5f7;color:#172b4d;font-size:14px;cursor:pointer;">Cancel</button>' +
            '<button id="lock-apply" style="padding:8px 20px;border:none;border-radius:4px;background:#0052CC;color:#fff;font-size:14px;font-weight:500;cursor:pointer;">Apply</button>' +
            '</div>';

        modal.innerHTML = header + modeSection + subjectsSection + footer;
        overlay.appendChild(modal);
        document.body.appendChild(overlay);

        // Events
        overlay.addEventListener('click', function(e) { if (e.target === overlay) overlay.remove(); });
        document.getElementById('lock-close').addEventListener('click', function() { overlay.remove(); });
        document.getElementById('lock-cancel').addEventListener('click', function() { overlay.remove(); });
        document.getElementById('lock-apply').addEventListener('click', applyRestrictions);

        document.getElementById('lock-mode').addEventListener('change', function() {
            var mode = this.value;
            this.style.backgroundColor = modeColors[mode];
            document.getElementById('lock-mode-desc').textContent = modeDescs[mode];
            document.getElementById('lock-subjects-section').style.display = mode === 'none' ? 'none' : 'block';
        });

        document.getElementById('lock-add-btn').addEventListener('click', function() {
            var name = document.getElementById('lock-search').value.trim();
            if (!name) return;
            var isGroup = name.toLowerCase().includes('group');
            currentSubjects.push({ type: isGroup ? 'group' : 'user', name: name, level: document.getElementById('lock-add-level').value });
            document.getElementById('lock-search').value = '';
            renderSubjects();
        });

        document.getElementById('lock-search').addEventListener('keydown', function(e) {
            if (e.key === 'Enter') { e.preventDefault(); document.getElementById('lock-add-btn').click(); }
        });

        renderSubjects();
    }

    function renderSubjects() {
        var list = document.getElementById('lock-subjects-list');
        if (!list) return;
        list.innerHTML = '';

        currentSubjects.forEach(function(s, i) {
            var row = document.createElement('div');
            row.style.cssText = 'display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid #f4f5f7;';

            var icon = s.type === 'group'
                ? '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#0052CC" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 21v-1a6 6 0 0 1 12 0v1"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/><path d="M21 21v-1a4 4 0 0 0-3-3.85"/></svg>'
                : '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#0052CC" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 21v-1a6 6 0 0 1 12 0v1"/></svg>';

            var levelLabel = (s.level === 'view') ? 'Can view' : 'Can view and edit';

            row.innerHTML = '<div style="display:flex;align-items:center;gap:8px;">' + icon +
                '<span style="font-size:14px;color:#172b4d;">' + s.name + '</span></div>' +
                '<div style="display:flex;align-items:center;gap:12px;">' +
                '<select data-idx="' + i + '" class="lock-level-select" style="padding:4px 8px;border:1px solid #dfe1e6;border-radius:4px;font-size:13px;background:#fff;">' +
                '<option value="edit"' + (s.level !== 'view' ? ' selected' : '') + '>Can view and edit</option>' +
                '<option value="view"' + (s.level === 'view' ? ' selected' : '') + '>Can view</option></select>' +
                '<a href="#" data-idx="' + i + '" class="lock-remove" style="font-size:13px;color:#DE350B;text-decoration:none;">Remove</a></div>';

            list.appendChild(row);
        });

        // Bind level change
        list.querySelectorAll('.lock-level-select').forEach(function(sel) {
            sel.addEventListener('change', function() {
                currentSubjects[parseInt(this.dataset.idx)].level = this.value;
            });
        });

        // Bind remove
        list.querySelectorAll('.lock-remove').forEach(function(link) {
            link.addEventListener('click', function(e) {
                e.preventDefault();
                currentSubjects.splice(parseInt(this.dataset.idx), 1);
                renderSubjects();
            });
        });
    }

    function applyRestrictions() {
        var mode = document.getElementById('lock-mode').value;
        var applyBtn = document.getElementById('lock-apply');
        applyBtn.textContent = 'Applying...';
        applyBtn.disabled = true;

        fetch(HANDLER_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ page: pageRef, mode: mode, subjects: currentSubjects })
        })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                updateIcon(mode);
                document.getElementById('xwiki-lock-popup').remove();
                var toast = document.createElement('div');
                toast.style.cssText = 'position:fixed;bottom:20px;right:20px;background:#00875A;color:#fff;padding:12px 20px;border-radius:4px;font-size:14px;z-index:999999;box-shadow:0 4px 12px rgba(0,0,0,0.15);';
                toast.textContent = '\u2713 ' + (mode === 'none' ? 'Restrictions removed' : 'Restrictions applied');
                document.body.appendChild(toast);
                setTimeout(function() { toast.remove(); }, 3000);
            } else {
                alert('Error: ' + (data.error || 'Unknown error'));
                applyBtn.textContent = 'Apply';
                applyBtn.disabled = false;
            }
        })
        .catch(function(err) {
            alert('Failed: ' + err.message);
            applyBtn.textContent = 'Apply';
            applyBtn.disabled = false;
        });
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', injectIcon);
    else injectIcon();
})();
