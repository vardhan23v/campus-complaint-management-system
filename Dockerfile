# ---------------------------------------------------------------------------
# Nitte Resolve - container image (used by Railway, works with any Docker host)
#
# Stage 1 builds the WAR with Maven and Java 21; stage 2 runs it on
# Tomcat 10.1 as the ROOT application, so the site is served at "/".
#
# Runtime environment variables:
#   PORT              HTTP port (set automatically by Railway; default 8080)
#   DB_URL            JDBC URL, e.g. jdbc:mysql://host:3306/campus_complaint_db?...
#   DB_USER           database user
#   DB_PASSWORD       database password
#   UPLOAD_DIRECTORY  folder for complaint images (mount a volume here)
#   TZ                time zone of the application, e.g. Asia/Kolkata
# ---------------------------------------------------------------------------

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM tomcat:10.1-jdk21-temurin
ENV TZ=Asia/Kolkata \
    UPLOAD_DIRECTORY=/data/uploads \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"
RUN rm -rf /usr/local/tomcat/webapps/* && mkdir -p /data/uploads
COPY --from=build /build/target/campus-complaint.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
# Railway assigns the port at runtime: point Tomcat's HTTP connector at $PORT.
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" /usr/local/tomcat/conf/server.xml && exec catalina.sh run"]
