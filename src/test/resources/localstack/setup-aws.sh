#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2
export PAGER=

aws --endpoint-url=http://localhost:4566 s3 --region eu-west-2 ls s3://cpg-s3-bucket-name || aws --endpoint-url=http://localhost:4566 --region=eu-west-2 s3 mb s3://cpg-s3-bucket-name

echo "Configured S3"

exit 0