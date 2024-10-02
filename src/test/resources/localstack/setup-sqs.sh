#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2
export PAGER=

aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name court-case-matcher-dlq
aws --endpoint-url http://localhost:4566 sqs create-queue --queue-name court-case-matcher-queue

aws --endpoint-url=http://localhost:4566 sqs set-queue-attributes --queue-url "http://localhost:4566/000000000000/court-case-matcher-queue" --attributes '{"RedrivePolicy":"{\"maxReceiveCount\":\"1\", \"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:court-case-matcher-dlq\"}"}'

aws --endpoint-url=http://localhost:4566 sns create-topic --name court-case-events-topic

aws --endpoint-url=http://localhost:4566 sns subscribe --topic-arn "arn:aws:sns:eu-west-2:000000000000:court-case-events-topic" --protocol "sqs" --notification-endpoint "arn:aws:sqs:eu-west-2:000000000000:court-case-matcher-queue"

aws --endpoint-url=http://localhost:4566 s3 --region eu-west-2 ls s3://cpg-s3-bucket-name || aws --endpoint-url=http://localhost:4566 --region=eu-west-2 s3 mb s3://cpg-s3-bucket-name

echo "Configured S3, SNS and SQS"

exit 0
