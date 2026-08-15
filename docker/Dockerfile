FROM mcr.microsoft.com/playwright/java:v1.49.0-noble

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn test-compile

ENTRYPOINT ["mvn", "test", "-DsuiteXmlFile=src/test/resources/testng/smoke.xml"]
