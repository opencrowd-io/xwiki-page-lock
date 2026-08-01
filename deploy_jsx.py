import requests

base = 'https://xwiki.notropia.co'
auth = ('kiro2', 'Kiro@123')

# Delete existing JSX objects
for i in range(5):
    r = requests.delete(f'{base}/rest/wikis/xwiki/spaces/XWikiLock/spaces/Code/pages/LockIconJSX/objects/XWiki.JavaScriptExtension/{i}', auth=auth, timeout=15)
    if r.status_code == 204:
        print(f'Deleted object {i}')

# Read JS file
with open('src/LockIcon.js') as f:
    js = f.read()

# Create object with CDATA
url = f'{base}/rest/wikis/xwiki/spaces/XWikiLock/spaces/Code/pages/LockIconJSX/objects'
xml = ('<?xml version="1.0" encoding="UTF-8"?>'
       '<object xmlns="http://www.xwiki.org">'
       '<className>XWiki.JavaScriptExtension</className>'
       '<property name="use" type="String"><value>always</value></property>'
       '<property name="cache" type="String"><value>long</value></property>'
       '<property name="parse" type="Boolean"><value>0</value></property>'
       '<property name="code" type="TextArea"><value><![CDATA[' + js + ']]></value></property>'
       '</object>')

r = requests.post(url, auth=auth, headers={'Content-Type': 'application/xml'}, data=xml.encode('utf-8'), timeout=30)
print(f'Created JSX: {r.status_code}')
