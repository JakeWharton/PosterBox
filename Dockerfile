# If you change this major version, change the --multi-release jdeps flag below
FROM openjdk:18-alpine AS build

RUN apk add \
      # Install an Alpine-aware copy of Node. The version Kotlin would download targets glibc.
      nodejs \
      # Binutils provides objcopy binary which is used by --strip-debug jlink flag.
      binutils \
      # Gradle build runs git to embed SHA.
      git \
    ;
WORKDIR /app

# Cache the Gradle binary separately from the build.
COPY gradlew ./
COPY gradle/wrapper ./gradle/wrapper
RUN ./gradlew --version

COPY gradle ./gradle
COPY build.gradle gradle.properties settings.gradle ./
COPY src ./src
RUN ./gradlew assemble -PskipNodeDownload

# Find modules which are required.
RUN jdeps \
      --ignore-missing-deps \
      # Keep in sync with major version of container above.
      --multi-release 18 \
      --print-module-deps \
      --class-path build/install/posterbox/lib/* \
    # Split comma-separated items into lines.
    | tr ',' '\n' \
    # Used only by Ktor's HOCON parser which we do not use.
    | grep -v java.desktop \
    # Used only by kotlinx.coroutines debug agent which we do not use.
    | grep -v java.instrument \
    # Used only by kotlinx.coroutines debug agent which we do not use.
    | grep -v jdk.unsupported \
    # Used only by Ktor for detecting IntelliJ IDEA debugger which we do not use.
    # BUT our HTTP server cannot respond to requests without this module for some reason...
    #| grep -v java.management \
    # Join lines with comma.
    | tr '\n' ',' \
    # Replace trailing comma with a newline.
    | sed 's/,$/\n/' \
    # Print to stdout AND write to this file.
    | tee jdeps.txt \
  ;

# Build custom minimal JRE with only the modules we need.
RUN jlink \
      --verbose \
      --compress 2 \
      --strip-debug \
      --no-header-files \
      --no-man-pages \
      --output jre \
      --add-modules $(cat jdeps.txt) \
   ;

FROM alpine:3.16.0
EXPOSE 9931
HEALTHCHECK --interval=1m --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9931/ || exit 1

COPY --from=build /app/jre /jre
ENV JAVA_HOME="/jre"

COPY --from=build /app/build/install/posterbox/ /app

ENTRYPOINT ["/app/bin/posterbox", "/config/config.toml"]
