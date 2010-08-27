1. mvn hpi:run

I don't know why, but if you run "mvn hpi:run" it will include xercesImpl-2.2.1, which conflicts with Aixom. But this dependency is "hidden", it is not shown in "mvn dependency:tree". But since this jar is not included in package, my workaround is to modified xercesImpl-2.2.1.pom to "relocation" to relocate to v2.6.2

Path to dependency:
      1) org.jvnet.hudson.plugins.fortify360:fortify360:hpi:1.4-SNAPSHOT
      2) org.jvnet.hudson.main:hudson-core:jar:1.323
      3) commons-jelly:commons-jelly-tags-xml:jar:1.1
      4) xerces:xerces:jar:2.2.1

2. mvn hpi:run

Due to ClassLoader issue, and somehow Hudson included activation-1.1, which is already covered by JRE 1.6. The activation-1.1 will cause LinkageError (seems at production environment, only for hpi:run). My workaround is to removed the activation-1.1 jar in Hudson WEB-INF/lib