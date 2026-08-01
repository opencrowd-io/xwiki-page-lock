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

// Determine target for rights
def isSpaceHome = doc.name == "WebHome"
def rightsClass = isSpaceHome ? "XWiki.XWikiGlobalRights" : "XWiki.XWikiRights"
def targetDoc = isSpaceHome ? wiki.getDocument(doc.space + ".WebPreferences") : doc

// Remove existing rights objects
def existingObjects = targetDoc.getObjects(rightsClass)
if (existingObjects) {
    existingObjects.each { obj ->
        if (obj != null) targetDoc.removeObject(obj)
    }
}

if (mode == "none") {
    targetDoc.save("Restrictions removed via Lock extension")
    println JsonOutput.toJson([success: true, message: "Restrictions removed", level: "none"])
    return
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

println JsonOutput.toJson([success: true, message: "Restrictions applied", level: mode, isSpaceLevel: isSpaceHome])
