FROM centos:7
RUN yum update -y

# for javacpp
RUN yum install -y gcc

# java runtime only (without -devel)
RUN yum install -y java-1.8.0-openjdk

# https://spring.io/guides/gs/spring-boot-docker/
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
