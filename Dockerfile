FROM clojure:openjdk-17-lein as build-env

ADD . /app
WORKDIR /app

RUN lein uberjar

# -----------------------------------------------------------------------------

FROM eclipse-temurin:17

RUN groupadd -r commentator && useradd -r -s /bin/false -g commentator commentator

RUN mkdir /app
COPY --from=build-env --chown=commentator:commentator /app/target/uberjar/commentator-*-standalone.jar /app/commentator.jar
ENV COMMENTATOR_CONFIGURATION=/app/config.edn
USER commentator

ENTRYPOINT ["java", "-ea", "-XX:+AlwaysPreTouch", "-XX:MaxRAMPercentage=90", "-cp", "/app/commentator.jar"]

CMD ["commentator.core"]
