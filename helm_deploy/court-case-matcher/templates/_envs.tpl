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

  - name: OFFENDER_SEARCH_USE_DOB_WITH_PNC
    value: "true"

  - name: CRIME_PORTAL_GATEWAY_S3_BUCKET
    valueFrom:
      secretKeyRef:
        name: crime-portal-gateway-s3-credentials
        key: bucket_name

  - name: OFFENDER_SEARCH_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: nomis-oauth-client-id

  - name: PERSON_RECORD_SEARCH_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: nomis-oauth-client-id

  - name: PERSON_RECORD_SEARCH_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-secrets
        key: nomis-oauth-client-secret

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

  - name: AWS_SQS_COURT_CASE_MATCHER_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: court-cases-queue-credentials
        key: sqs_name

  - name: AWS_SQS_COURT_CASE_MATCHER_ENDPOINT_URL
    valueFrom:
      secretKeyRef:
        name: court-cases-queue-credentials
        key: sqs_id

  - name: AWS_SQS_COURT_CASE_MATCHER_DLQ_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-queue-dead-letter-queue-credentials
        key: sqs_name

  - name: AWS_SQS_COURT_CASE_MATCHER_DLQ_ENDPOINT_URL
    valueFrom:
      secretKeyRef:
        name: court-case-matcher-queue-dead-letter-queue-credentials
        key: sqs_id
{{- end -}}
