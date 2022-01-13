---
title: Get and launch Commentator
weight: 1
disableToc: false
---

## Get Commentator jar file

You can download the Commentator jar from [Github](https://github.com/mcorbin/commentator/releases). Commentator is tested under Java 17 (LTS).

You can also build Commentator yourself. You will need to do that:

- [Leiningen](https://leiningen.org/), the Clojure build tool.
- Java 11.

Then, clone the [Git repository](https://github.com/mcorbin/commentator). You can now build the project with `lein uberjar`.

The resulting jar will be in `target/uberjar/commentator-<version>-standalone.jar`

## Launch Commentator

Commentator needs a [configuration file](/howto/configuration/). You need to set the environment variable `COMMENTATOR_CONFIGURATION` to the path of this file.

### Using Docker

The Docker image is available on the [Docker Hub](https://hub.docker.com/r/mcorbin/commentator).
You will need to mount the configuration file as a volume (or inject it into the running container somehow) in order to make it work.
