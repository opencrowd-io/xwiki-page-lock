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

def rightsObjects = doc.getObjects("XWiki.XWikiRights")
if (!rightsObjects || rightsObjects.isEmpty()) {
    println JsonOutput.toJson([level: "none", subjects: []])
    return
}

def hasView = false
def hasEdit = false
def subjects = []

rightsObjects.each { obj ->
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
                if (name) subjects << [type: "group", name: name]
            }
        }
        if (users) {
            users.split(",").each { u ->
                def name = u.trim().replaceAll("XWiki\\.", "")
                if (name) subjects << [type: "user", name: name]
            }
        }
    }
}

def level = "none"
if (hasView) level = "full"
else if (hasEdit) level = "edit"

println JsonOutput.toJson([level: level, subjects: subjects, page: pageRef])
