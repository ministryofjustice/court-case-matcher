# PIC-4207 recovering lost data

## Overview of problem and solution

### Problem
All requests to court-case-matcher/hearing/{id}/ returned a 404 between 19 and 30 September because of a Spring Boot upgrade which changed the
default behaviour. Previously requests with a trailing slash were mapped to an endpoint defined without a trailing slash.
Fortunately a filter wrote the message payloads to S3 before returning the 404

### Solution

This will involve pausing the consumption of new messages from Common Platform, which will delay hearings from appearing on PACFS.

We are able to retrieve the path to the latest message payload for each hearing affected with [this AppInsights query](https://portal.azure.com/#@747381f4-e81f-4a43-bf68-ced6a1e14edf/blade/Microsoft_OperationsManagementSuite_Workspace/Logs.ReactView/resourceId/%2Fsubscriptions%2Fa5ddf257-3b21-4ba9-a28c-ab30f751b383%2FresourceGroups%2Fnomisapi-prod-rg%2Fproviders%2FMicrosoft.Insights%2Fcomponents%2Fnomisapi-prod/source/LogsBlade.AnalyticsShareLinkToQuery/q/H4sIAAAAAAAAA22QzU6EQBCE7z5Fhws%252FAdFkr5xMTPaiRr0ZQ0amhVmZGexp2NX48A4swmbjtbrq6%252B4i%252FOzRsbv4gX2DhFC1tpflo23xTmiEooCgsj1x1qAgZeoMBzScEVaoBqRgCRK6vuUbK6fQ5mqzTGyHJFhZUy7Ih%252FunZ8iTZIzjgdFImPlbCQUwKV16MQrzMGXreJxErmsVRz21aTib8zB%252BuX6NY0%252BxJJHg7QtYaf%252BQ0J0XO7I7rHhlpzDGV096ctxW%252BoTrtfbebwRBdanFIVq9STzyF5Z376wy8KGMLJQxfj2TqNCBNefYYxEe5ESN4HHEbq%252B4geBWtb70Lj9p4t1LZmpqLSIAJwaUwBYukyCd9AkTHQlBOsOnMv75%252Bw%252F6CyZi4krxAQAA/timespan/2024-09-19T14%3A00%3A55.000Z%2F2024-09-30T16%3A31%3A55.000Z)
// TODO check query is up to date

Export the results of this query to CSV, then write an endpoint on `court-case-matcher`. 

- Subscribe `court-case-matcher` to an empty queue to avoid race conditions when processing updates
- Post the CSV to the `/replay404hearings` endpoint.
- Each hearing will be saved to the database unless a more recent version exists
- Once processing has completed, subscribe `court-case-matcher` to the court-case-events queue once more. 

## How to replay hearings

The endpoint `/replay404Hearings` is only available from within the k8s cluster.  
Set up a port-forward from your machine to the `court-case-matcher` application like this: 
`kubectl -n court-probation-dev port-forward deployments/court-case-matcher 8080:8080`

This will forward `http://localhost:8080` on your machine to the deployed `court-case-matcher`

You can then post a file in the format of [test-hearings.csv](src/test/resources/replay404hearings/test-hearings.csv) like this:

`curl -X post file=@src/main/resources/replay404hearings/hearings.csv http://localhost:8180/replay404Hearings`
// TODO check the above works

## Dry run mode

In dry run mode, the hearing will be retrieved from S3 and the last modified date will be checked against the date the hearing was written to S3. The hearing will not be saved.
Log statements and telemetry will indicate if the hearing would have been updated, created or discarded as a more recent version is in the database.


### modifying environment variables on running instances

```
kubectl set env deployment/court-case-matcher REPLAY404_DRY_RUN=false -n namespace
```

TODO

Convert file upload to use csv mime type and simplify requests

Log dry run as property in all events

Get test passing with dry run on

Test calls to telemetry?

Check speed, consider multi threading