FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk-jammy AS builder

ARG BUILD_NUMBER
ENV BUILD_NUMBER ${BUILD_NUMBER:-1_0_0}

WORKDIR /app
ADD . .
RUN ./gradlew --no-daemon assemble

FROM eclipse-temurin:21-jre-jammy
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

ARG BUILD_NUMBER
ENV BUILD_NUMBER ${BUILD_NUMBER:-1_0_0}

ENV TZ=Europe/London
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

RUN groupadd --gid 2000 --system appgroup && \
    adduser --uid 2000 --system appuser --gid 2000

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup /app/build/libs/court-case-matcher-*.jar /app/court-case-matcher.jar
COPY --from=builder --chown=appuser:appgroup /app/build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY --from=builder --chown=appuser:appgroup /app/applicationinsights.json /app
COPY --from=builder --chown=appuser:appgroup /app/run.sh /app

USER 2000

ENTRYPOINT ["/bin/sh", "/app/run.sh"]
