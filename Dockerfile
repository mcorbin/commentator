FROM clojure:openjdk-11-lein as build-env

ADD . /app
WORKDIR /app

RUN lein uberjar

# -----------------------------------------------------------------------------

FROM adoptopenjdk/openjdk11:alpine-jre

RUN addgroup -S commentator && \
    adduser -s /bin/false -G commentator -S commentator && \
    apk add --update-cache git && \ 
    rm -rf /var/cache/apk/*
RUN mkdir /app
COPY --from=build-env --chown=commentator:commentator /app/target/uberjar/commentator-*-standalone.jar /app/commentator.jar
USER commentator

ENTRYPOINT ["java"]
CMD ["-jar", "/app/commentator.jar"]
