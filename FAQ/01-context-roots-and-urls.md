# FAQ: Context Roots and Application URLs in WebSphere Liberty

## Question

Why does my Liberty application use a URL such as:

~~~text
http://localhost:9081/hello/hello
~~~

What do the two hello values mean, and how can I change the URL?

## Short answer

The URL is composed of:

~~~text
http://HOST:PORT/CONTEXT_ROOT/SERVLET_PATH
~~~

For the example:

~~~text
http://localhost:9081/hello/hello
                     │      │
                     │      └── Servlet path
                     └───────── Application context root
~~~

## Where does the first hello come from?

The first hello is the application context root.

When an application is deployed from the dropins directory, Liberty commonly derives the context root from the WAR filename:

~~~text
hello.war → /hello
~~~

In the Maven project, this filename is controlled by:

~~~xml
<finalName>hello</finalName>
~~~

Therefore, Maven creates:

~~~text
target/hello.war
~~~

and Liberty uses /hello as the context root when the WAR is deployed through dropins.

The context root is not created by the Java servlet annotation.

## Where does the second hello come from?

The second hello is the servlet path. It comes from the servlet mapping in the Java source:

~~~java
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    // ...
}
~~~

Liberty combines the context root and servlet path:

~~~text
/hello + /hello = /hello/hello
~~~

## How do I change the servlet path?

Change the annotation:

~~~java
@WebServlet("/greeting")
~~~

After rebuilding and redeploying the application, the URL becomes:

~~~text
http://localhost:9081/hello/greeting
~~~

The context root remains /hello; only the servlet path changes.

## How do I change the context root?

### Option 1: Change the WAR filename

Change the Maven final name:

~~~xml
<finalName>greeting</finalName>
~~~

Maven then creates:

~~~text
greeting.war
~~~

When deployed through dropins, Liberty commonly uses:

~~~text
/greeting
~~~

The URL becomes:

~~~text
http://localhost:9081/greeting/hello
~~~

### Option 2: Configure the context root explicitly

Use a webApplication element in server.xml:

~~~xml
<webApplication
    id="hello"
    name="hello"
    contextRoot="/greeting"
    location="hello.war" />
~~~

For explicit configuration, place the WAR in the server's apps directory rather than dropins, for example:

~~~dockerfile
COPY --chown=1001:0 server.xml /config/
COPY --chown=1001:0 target/hello.war /config/apps/
~~~

The explicit contextRoot takes precedence over the WAR filename.

## How can I use exactly /hello?

There are two common designs.

### Root application with a hello servlet

Configure the application context root as / and keep the servlet mapping as /hello:

~~~xml
<webApplication
    id="hello"
    name="hello"
    contextRoot="/"
    location="hello.war" />
~~~

~~~java
@WebServlet("/hello")
~~~

The resulting URL is:

~~~text
http://localhost:9081/hello
~~~

### Hello application with a root servlet

Keep the context root as /hello and map the servlet to the root of the application:

~~~java
@WebServlet("/")
~~~

This is commonly accessed as:

~~~text
http://localhost:9081/hello/
~~~

The trailing slash is the clearer form for the application root.

## Important distinction

The Maven finalName should be a filename such as hello or greeting. It should not be /.

~~~text
finalName       → controls the WAR filename
contextRoot     → controls the application URL prefix
@WebServlet     → controls the servlet URL path
~~~

## Related IBM documentation

- [Deploying applications in Liberty](https://www.ibm.com/docs/en/was-liberty/core?topic=deploying-applications-in-liberty)
- [Deploying a web application to Liberty](https://www.ibm.com/docs/en/was-liberty/core?topic=liberty-deploying-web-application)

