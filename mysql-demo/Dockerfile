FROM openjdk:17-jdk-slim
WORKDIR /opt/app
COPY target/*.jar /opt/app/app.jar
ENTRYPOINT ["java","-jar","app.jar"]