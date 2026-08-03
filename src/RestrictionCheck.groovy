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
    println JsonOutput.toJson([level: "none", subjects: []])
    return
}

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
        def levels = obj.get("levels") ?: ""
        def groups = obj.get("groups") ?: ""
        def users = obj.get("users") ?: ""
        def allow = obj.getIntValue("allow", 1)

        if (allow == 1) {
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
        def webPrefRef = spaceName + ".WebPreferences"
        def webPrefDoc = wiki.getDocument(webPrefRef)
        if (webPrefDoc != null && !webPrefDoc.isNew()) {
            def spaceRights = webPrefDoc.getObjects("XWiki.XWikiGlobalRights")
            if (spaceRights && !spaceRights.isEmpty()) {
                parseRights(spaceRights)
                if (hasView || hasEdit) restrictionSource = "space"
            }
        }
    } catch (e) {
        // Ignore errors checking space rights
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
    subjects: uniqueSubjects,
    page: pageRef,
    source: restrictionSource,
    restrictedBy: restrictedBy
])
