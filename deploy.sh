#!/bin/bash
# Deploy xWiki Page Lock extension to a running xWiki instance.
# Usage: ./deploy.sh [BASE_URL] [USERNAME] [PASSWORD]

BASE_URL="${1:-https://xwiki.notropia.co}"
USERNAME="${2:-kiro2}"
PASSWORD="${3:-Kiro@123}"

echo "Deploying xWiki Page Lock extension to $BASE_URL..."
echo ""

# Helper: create or update a page via REST API
create_page() {
    local space="$1"
    local page="$2"
    local content_file="$3"
    local title="$4"

    echo "  → Creating $space/$page..."

    local content
    content=$(cat "$content_file")

    # URL encode the content for XML
    local xml_content
    xml_content=$(echo "$content" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g')

    curl -s -u "$USERNAME:$PASSWORD" \
        -X PUT \
        -H "Content-Type: application/xml" \
        -d "<?xml version=\"1.0\" encoding=\"UTF-8\"?><page xmlns=\"http://www.xwiki.org\"><title>$title</title><syntax>plain/1.0</syntax><content>$xml_content</content></page>" \
        "$BASE_URL/rest/wikis/xwiki/spaces/$space/pages/$page" \
        -o /dev/null -w "%{http_code}"

    echo ""
}

# Create the pages
echo "[1/4] RestrictionHandler (Groovy backend)..."
create_page "XWikiLock.Code" "RestrictionHandler" "src/RestrictionHandler.groovy" "Restriction Handler"

echo "[2/4] RestrictionCheck (Groovy state check)..."
create_page "XWikiLock.Code" "RestrictionCheck" "src/RestrictionCheck.groovy" "Restriction Check"

echo "[3/4] LockIconJSX (JavaScript)..."
create_page "XWikiLock.Code" "LockIconJSX" "src/LockIcon.js" "Lock Icon JSX"

echo "[4/4] LockStyleSSX (CSS)..."
create_page "XWikiLock.Code" "LockStyleSSX" "src/LockStyle.css" "Lock Style SSX"

echo ""
echo "Done! Pages created at $BASE_URL/bin/view/XWikiLock/Code/"
echo ""
echo "NEXT STEPS:"
echo "  1. Go to $BASE_URL/bin/edit/XWikiLock/Code/LockIconJSX"
echo "     → Add object: XWiki.JavaScriptExtension"
echo "     → Set 'use' = 'always', 'cache' = 'long'"
echo "     → Put the JS code in the 'code' field"
echo ""
echo "  2. Go to $BASE_URL/bin/edit/XWikiLock/Code/LockStyleSSX"
echo "     → Add object: XWiki.StyleSheetExtension"
echo "     → Set 'use' = 'always'"
echo "     → Put the CSS code in the 'code' field"
echo ""
echo "  3. Test: Navigate to any wiki page → you should see a 🔓 icon"
