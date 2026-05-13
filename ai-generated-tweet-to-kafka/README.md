Logback initializes in two phases
JVM starts
└─► Logback init  ◄── contextName evaluated HERE
└─► Spring starts
└─► environment prepared  ◄── springProperty resolves HERE
└─► application.properties loaded

The key difference is the filename: your working config is named logback-spring.xml, not logback.xml.
logback.xml        →  loaded by Logback directly, before Spring
logback-spring.xml →  loaded by Spring Boot, after environment is prepared
When Spring Boot loads logback-spring.xml, it has already processed application.properties — so spring.application.name is available when <springProperty> and <contextName> are evaluated. That's why it works.
So the rule of thumb is:
FileLoaded by<springProperty> works?logback.xmlLogback directly❌ Nologback-spring.xmlSpring Boot✅ Yes

Logback's severity order is:
TRACE < DEBUG < INFO < WARN < ERROR < OFF
A logger only prints messages at its level or above. So setting WARN will print WARN and ERROR but suppress INFO, DEBUG, and TRACE.

