FROM openjdk:19-jdk-slim-buster
LABEL maintainer="HMPPS Digital Studio <info@digital.justice.gov.uk>"

ENV TZ=Europe/London
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone

RUN groupadd --gid 2000 --system appgroup && \
    adduser --uid 2000 --system appuser --gid 2000

RUN mkdir -p /app
WORKDIR /app

COPY build/libs/court-case-matcher-*.jar /app/court-case-matcher.jar
COPY build/libs/applicationinsights-agent*.jar /app/agent.jar
COPY applicationinsights.json /app
COPY run.sh /app

RUN chown -R appuser:appgroup /app

USER 2000

ENTRYPOINT ["/bin/sh", "/app/run.sh"]
