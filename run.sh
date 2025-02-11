./gradlew bootJar
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:6332 -jar build/libs/ds-spring-user-framework-demo-1.0.1-SNAPSHOT.jar --spring.profiles.active=local