# FAQ: Where Is Liberty's Default Welcome HTML?

## Question

Where can I find the HTML file displayed at:

~~~text
http://localhost:9081/
~~~

## Answer

The default Liberty welcome page is usually not stored as an editable HTML file in the server's config directories. Liberty generates it internally through the HTTP dispatcher when no application is assigned to the root context /.

This is different from an application page packaged inside a WAR file.

## Inspect the returned HTML

Save the response to a file on your computer:

~~~bash
curl -s http://localhost:9081/ -o welcome.html
~~~

Open it on macOS:

~~~bash
open welcome.html
~~~

You can also inspect the beginning of the response directly:

~~~bash
curl -s http://localhost:9081/ | head
~~~

## Search the container

You can search the Liberty installation and configuration directories:

~~~bash
docker exec hello-liberty-war sh -c \
  'find /opt/ibm/wlp /config -type f \( -iname "*.html" -o -iname "*welcome*" \) 2>/dev/null'
~~~

Finding no standalone welcome HTML file is expected because the page is generated internally.

## How to customize the page

To customize the content, deploy your own web application at the root context /. For example:

~~~xml
<webApplication
    id="hello"
    name="hello"
    contextRoot="/"
    location="hello.war" />
~~~

Your application can then contain an index.html or a servlet that produces the root page.

## Related documentation

- [HTTP Dispatcher](https://www.ibm.com/docs/en/was-liberty/base?topic=configuration-httpdispatcher)

