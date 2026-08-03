import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def wiki = xwiki
def context = xcontext
def req = request
def resp = response

resp.setContentType("application/json")

// Only accept POST
if (req.getMethod() != "POST") {
    println JsonOutput.toJson([success: false, error: "POST required"])
    return
}

// Parse request body
def body = req.getReader().text
def params = new JsonSlurper().parseText(body)

def pageRef = params.page
def mode = params.mode
def subjects = params.subjects ?: []
def targetLevel = params.targetLevel ?: "auto" // "page", "space", or "auto"

if (!pageRef || !mode) {
    println JsonOutput.toJson([success: false, error: "Missing page or mode"])
    return
}

def doc = wiki.getDocument(pageRef)
if (doc == null || doc.isNew()) {
    println JsonOutput.toJson([success: false, error: "Page not found"])
    return
}

// Check permissions
if (!doc.hasAccessLevel("edit")) {
    println JsonOutput.toJson([success: false, error: "No edit permission"])
    return
}

if (mode == "full" && !doc.hasAccessLevel("admin")) {
    println JsonOutput.toJson([success: false, error: "Admin permission required for full restriction"])
    return
}

// Determine target: always manage the space-level WebPreferences
// This ensures restrictions work for both the page and its children
def spaceName = doc.space
def webPrefRef = spaceName + ".WebPreferences"
def webPrefDoc = wiki.getDocument(webPrefRef)

// For page-level, use the page itself
def isSpaceLevel = (doc.name == "WebHome" || targetLevel == "space")
def rightsClass = isSpaceLevel ? "XWiki.XWikiGlobalRights" : "XWiki.XWikiRights"
def targetDoc = isSpaceLevel ? webPrefDoc : doc

// Also clean space-level rights if we're removing all restrictions
if (mode == "none") {
    // Remove page-level rights
    def pageRights = doc.getObjects("XWiki.XWikiRights")
    if (pageRights) {
        pageRights.each { obj -> if (obj != null) doc.removeObject(obj) }
        doc.save("Page restrictions removed via Lock extension")
    }
    // Remove space-level rights
    if (webPrefDoc != null && !webPrefDoc.isNew()) {
        def spaceRights = webPrefDoc.getObjects("XWiki.XWikiGlobalRights")
        if (spaceRights) {
            spaceRights.each { obj -> if (obj != null) webPrefDoc.removeObject(obj) }
            webPrefDoc.save("Space restrictions removed via Lock extension")
        }
    }
    println JsonOutput.toJson([success: true, message: "All restrictions removed", level: "none"])
    return
}

// Remove existing rights on the target
def existingObjects = targetDoc.getObjects(rightsClass)
if (existingObjects) {
    existingObjects.each { obj ->
        if (obj != null) targetDoc.removeObject(obj)
    }
}

// Build subjects list
def allUsers = []
def allGroups = ["XWiki.XWikiAdminGroup"]

def currentUser = context.user
if (currentUser && !currentUser.contains("XWikiGuest")) {
    allUsers << currentUser
}

def author = doc.author
if (author && !allUsers.contains(author)) {
    allUsers << author
}

subjects.each { s ->
    def ref = "XWiki.${s.name}"
    if (s.type == "group") {
        if (!allGroups.contains(ref)) allGroups << ref
    } else {
        if (!allUsers.contains(ref)) allUsers << ref
    }
}

def levels = (mode == "full") ? "view,edit" : "edit"

// Create new rights object
def rightsObj = targetDoc.newObject(rightsClass)
rightsObj.set("levels", levels)
rightsObj.set("groups", allGroups.join(","))
rightsObj.set("users", allUsers.join(","))
rightsObj.set("allow", 1)

targetDoc.save("Restrictions applied via Lock extension (mode: ${mode})")

println JsonOutput.toJson([success: true, message: "Restrictions applied", level: mode, isSpaceLevel: isSpaceLevel])
