FortifyClient is a sub-module of Fortify360 plugin

It is basically a wrapper around wsclient.jar and wsobjects.jar.

When Fortify360 plugin runs, it will dynamically loads this jar and the related wsclient.jar and wsobjects.jar. Since this jar is F360 version dependent, you will need to compile it against different versions of wsclient.jar and wsobjects.jar  Define the fortify.version in order to build the "fortifyclient-x.y.jar"

e.g.
mvn package -Dfortify.version=2.1
mvn package -Dfortify.version=2.5
mvn package -Dfortify.version=2.6

The mvn command line arguments are passed to Java as MAVEN_CMD_LINE_ARGS, test cases will read this environment variable and set different url and tokens during testing

And remember to put the related wsobjects.jar and wsclient.jar in "lib" directory

Test case will be skipped if the corresponding F360 server is started yet