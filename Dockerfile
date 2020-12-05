FROM clojure:openjdk-11-lein as build-env

ADD . /app
WORKDIR /app

RUN lein uberjar

# -----------------------------------------------------------------------------

from openjdk:11

RUN groupadd -r commentator && useradd -r -s /bin/false -g commentator commentator
RUN mkdir /app
COPY --from=build-env /app/target/uberjar/commentator-*-standalone.jar /app/commentator.jar

RUN chown -R commentator:commentator /app

RUN apt-get update && apt-get -y upgrade && apt-get install -y git
user commentator

ENTRYPOINT ["java"]

CMD ["-jar", "/app/commentator.jar"]
