import requests

base = 'https://xwiki.notropia.co'
auth = ('kiro2', 'Kiro@123')

# Fix RestrictionHandler — wrap in {{groovy}} macro
with open('src/RestrictionHandler.groovy') as f:
    groovy = f.read()

content = '{{groovy}}\n' + groovy + '\n{{/groovy}}'
xml = '<?xml version="1.0" encoding="UTF-8"?><page xmlns="http://www.xwiki.org"><title>RestrictionHandler</title><syntax>xwiki/2.1</syntax><content><![CDATA[' + content + ']]></content></page>'

r = requests.put(f'{base}/rest/wikis/xwiki/spaces/XWikiLock/spaces/Code/pages/RestrictionHandler',
    auth=auth, headers={'Content-Type': 'application/xml'}, data=xml.encode('utf-8'), timeout=30)
print(f'Handler: {r.status_code}')

# Fix RestrictionCheck — wrap in {{groovy}} macro
with open('src/RestrictionCheck.groovy') as f:
    groovy2 = f.read()

content2 = '{{groovy}}\n' + groovy2 + '\n{{/groovy}}'
xml2 = '<?xml version="1.0" encoding="UTF-8"?><page xmlns="http://www.xwiki.org"><title>RestrictionCheck</title><syntax>xwiki/2.1</syntax><content><![CDATA[' + content2 + ']]></content></page>'

r2 = requests.put(f'{base}/rest/wikis/xwiki/spaces/XWikiLock/spaces/Code/pages/RestrictionCheck',
    auth=auth, headers={'Content-Type': 'application/xml'}, data=xml2.encode('utf-8'), timeout=30)
print(f'Check: {r2.status_code}')
