The com.jfoenix.adapters.ReflectionHelper causes with Jav 16 an exception:
`java.base does not "opens java.lang.reflect" to unnamed module`

A workaround is to add the below JVM option:
`--add-opens java.base/java.lang.reflect=ALL-UNNAMED`

Better would be to get proper module support for the module.