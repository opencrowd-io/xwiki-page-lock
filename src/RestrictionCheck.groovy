import groovy.json.JsonOutput

def wiki = xwiki
def req = request

response.setContentType("application/json")

def pageRef = req.getParameter("page")
if (!pageRef) {
    println JsonOutput.toJson([error: "Missing page parameter", level: "none"])
    return
}

def doc = wiki.getDocument(pageRef)
if (doc == null || doc.isNew()) {
    println JsonOutput.toJson([level: "none", subjects: [], debug: "doc not found: " + pageRef])
    return
}

// Debug info
def debugSpace = doc.space
def debugFullName = doc.fullName
def debugName = doc.name

def hasView = false
def hasEdit = false
def subjects = []
def restrictedBy = ""
def restrictionSource = "none" // "page" or "space"

// Helper to parse rights objects
def parseRights = { rightsList ->
    if (!rightsList) return
    rightsList.each { obj ->
        if (obj == null) return
        def levels = (obj.get("levels") ?: obj.getStringValue("levels") ?: "").toString()
        def groups = (obj.get("groups") ?: obj.getStringValue("groups") ?: "").toString()
        def users = (obj.get("users") ?: obj.getStringValue("users") ?: "").toString()
        def allow = 1
        try { allow = obj.getIntValue("allow", 1) } catch (e) { allow = 1 }

        if (allow == 1 || allow == true) {
            if (levels.contains("view")) hasView = true
            if (levels.contains("edit")) hasEdit = true

            if (groups) {
                groups.split(",").each { g ->
                    def name = g.trim().replaceAll("XWiki\\.", "")
                    if (name && name != "XWikiAdminGroup") subjects << [type: "group", name: name]
                }
            }
            if (users) {
                users.split(",").each { u ->
                    def name = u.trim().replaceAll("XWiki\\.", "")
                    if (name) {
                        subjects << [type: "user", name: name]
                        if (!restrictedBy) restrictedBy = name
                    }
                }
            }
        }
    }
}

// 1. Check page-level rights (XWiki.XWikiRights on the page itself)
def pageRights = doc.getObjects("XWiki.XWikiRights")
if (pageRights && !pageRights.isEmpty()) {
    parseRights(pageRights)
    if (hasView || hasEdit) restrictionSource = "page"
}

// 2. If no page-level rights found, check space-level rights (WebPreferences)
if (!hasView && !hasEdit) {
    try {
        def spaceName = doc.space
        // In xWiki API, WebPreferences is accessed as a page within the space
        def webPrefDoc = wiki.getDocument(spaceName + ".WebPreferences")
        
        // If not found, try without the space prefix (for top-level spaces)
        if (webPrefDoc == null || webPrefDoc.isNew()) {
            webPrefDoc = wiki.getDocument("${spaceName}.WebPreferences")
        }
        
        if (webPrefDoc != null && !webPrefDoc.isNew()) {
            def spaceRights = webPrefDoc.getObjects("XWiki.XWikiGlobalRights")
            def spaceRightsCount = spaceRights ? spaceRights.size() : 0
            debugSpace = debugSpace + " | webPref found, rights count: " + spaceRightsCount
            if (spaceRights) {
                spaceRights.each { obj ->
                    if (obj == null) return
                    def levels = (obj.get("levels") ?: "").toString()
                    def groups = (obj.get("groups") ?: "").toString()
                    def users = (obj.get("users") ?: "").toString()
                    def allow = 1
                    try { allow = obj.getIntValue("allow", 1) } catch (e) {}
                    
                    if (allow == 1 && (levels.contains("view") || levels.contains("edit"))) {
                        if (levels.contains("view")) hasView = true
                        if (levels.contains("edit")) hasEdit = true
                        if (users) {
                            users.split(",").each { u ->
                                def name = u.trim().replaceAll("XWiki\\.", "")
                                if (name) { subjects << [type: "user", name: name]; if (!restrictedBy) restrictedBy = name }
                            }
                        }
                        if (groups) {
                            groups.split(",").each { g ->
                                def name = g.trim().replaceAll("XWiki\\.", "")
                                if (name && name != "XWikiAdminGroup") subjects << [type: "group", name: name]
                            }
                        }
                    }
                }
                if (hasView || hasEdit) restrictionSource = "space"
            }
            // Also check XWiki.XWikiRights on WebPreferences (some versions use this)
            if (!hasView && !hasEdit) {
                def altRights = webPrefDoc.getObjects("XWiki.XWikiRights")
                if (altRights && !altRights.isEmpty()) {
                    parseRights(altRights)
                    if (hasView || hasEdit) restrictionSource = "space"
                }
            }
        }
    } catch (e) {
        // Ignore errors
    }
}

// 3. Also check parent spaces (nested spaces)
if (!hasView && !hasEdit && doc.space.contains(".")) {
    try {
        def parts = doc.space.split("\\.")
        // Check each parent space's WebPreferences
        for (int i = parts.length - 1; i >= 1; i--) {
            def parentSpace = parts[0..i-1].join(".")
            def parentPrefRef = parentSpace + ".WebPreferences"
            def parentPrefDoc = wiki.getDocument(parentPrefRef)
            if (parentPrefDoc != null && !parentPrefDoc.isNew()) {
                def parentRights = parentPrefDoc.getObjects("XWiki.XWikiGlobalRights")
                if (parentRights && !parentRights.isEmpty()) {
                    parseRights(parentRights)
                    if (hasView || hasEdit) {
                        restrictionSource = "parent-space"
                        break
                    }
                }
            }
        }
    } catch (e) {
        // Ignore
    }
}

def level = "none"
if (hasView) level = "full"
else if (hasEdit) level = "edit"

// Remove duplicate subjects
def uniqueSubjects = subjects.unique { it.type + ":" + it.name }

println JsonOutput.toJson([
    level: level,
    subjects: uniqueSubjects.findAll { it.name },
    page: pageRef,
    source: restrictionSource,
    restrictedBy: restrictedBy
])
