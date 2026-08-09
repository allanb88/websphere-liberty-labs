# FAQ: Inspecting and Reverse-Engineering JAR and WAR Files

## Question

After creating a `.jar` or `.war` file, can I inspect or reverse-engineer its content?

## Short answer

Yes. A JAR or WAR file is an archive, similar to a ZIP file. You can list and extract its contents. However, compiled Java classes contain bytecode rather than the original Java source code.

## List the contents

Use `jar tvf` to display the files inside the archive:

~~~bash
jar tvf target/myproject-0.0.1-SNAPSHOT.jar
~~~

The options mean:

- `t` — list the archive contents
- `v` — show detailed information
- `f` — read from the specified file

You may see files such as:

~~~text
META-INF/MANIFEST.MF
com/example/App.class
application.properties
~~~

The command lists the archive. It does not display the Java source code.

## Extract the contents

Create a directory and extract the archive into it:

~~~bash
mkdir extracted
cd extracted
jar xvf ../target/myproject-0.0.1-SNAPSHOT.jar
~~~

You can also inspect a JAR without extracting it by using:

~~~bash
unzip -l target/myproject-0.0.1-SNAPSHOT.jar
~~~

## What can I inspect?

Depending on how the application was packaged, you may find:

- Compiled Java classes in `.class` files
- Configuration files such as `application.properties`
- HTML, CSS, JavaScript, images, and other static resources
- The manifest and build metadata
- Dependency JAR files
- A `web.xml` deployment descriptor

A WAR file is also an archive. It commonly contains web application files such as:

~~~text
WEB-INF/classes/     compiled application classes
WEB-INF/lib/         dependency JAR files
WEB-INF/web.xml      web application configuration
index.html           static web content
~~~

## Can I recover the original Java source code?

Usually, no. The `.class` files contain compiled bytecode, not the original `.java` files.

You can inspect bytecode with the JDK:

~~~bash
javap -c -p com.example.App
~~~

You can also use a Java decompiler to produce Java-like source code from `.class` files. The result may be useful for understanding the program, but it is not guaranteed to match the original source exactly. Comments, formatting, and some names are normally lost during compilation.

If the project publishes a separate source archive, it may be available as a file such as:

~~~text
myproject-0.0.1-SNAPSHOT-sources.jar
~~~

That archive contains the original `.java` files when the build was configured to publish them.

## Important security note

Anything packaged inside a JAR or WAR should be considered accessible to anyone who obtains the file. Do not package passwords, private keys, API tokens, or other secrets in the archive.

