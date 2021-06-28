    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "{{ .Values.spring.profile }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: OFFENDER_SEARCH_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: nomis-oauth-client-id

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: OFFENDER_SEARCH_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: nomis-oauth-client-secret

  - name: GATEWAY_JMS_USERNAME
    value: "not used - to be deleted"

  - name: GATEWAY_JMS_PASSWORD
    value: "not used - to be deleted"

  - name: AWS_SQS_CRIME_PORTAL_GATEWAY_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: crime-portal-gateway-queue-credentials
        key: access_key_id

  - name: AWS_SQS_CRIME_PORTAL_GATEWAY_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: crime-portal-gateway-queue-credentials
        key: secret_access_key

  - name: AWS_SQS_CRIME_PORTAL_GATEWAY_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: crime-portal-gateway-queue-credentials
        key: sqs_name

  - name: AWS_SQS_CRIME_PORTAL_GATEWAY_ENDPOINT_URL
    valueFrom:
      secretKeyRef:
        name: crime-portal-gateway-queue-credentials
        key: sqs_id

  - name: AWS_SQS_COURT_CASE_MATCHER_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-queue-credentials
        key: access_key_id

  - name: AWS_SQS_COURT_CASE_MATCHER_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-queue-credentials
        key: secret_access_key

  - name: AWS_SQS_COURT_CASE_MATCHER_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-queue-credentials
        key: sqs_name

  - name: AWS_SQS_COURT_CASE_MATCHER_ENDPOINT_URL
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-queue-credentials
        key: sqs_id

  - name: AWS_SQS_PROCESS_COURT_CASE_MATCHER_MESSAGES
    value: "{{ .Values.env.PROCESS_COURT_CASE_MATCHER_MESSAGES }}"
{{- end -}}
