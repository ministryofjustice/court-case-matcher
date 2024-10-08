# PIC-4207 recovering lost data

## Overview of problem and solution

### Problem
All requests to court-case-matcher/hearing/{id}/ returned a 404 between 19 and 30 September because of a Spring Boot upgrade which changed the
default behaviour. Previously requests with a trailing slash were mapped to an endpoint defined without a trailing slash.
Fortunately a filter wrote the message payloads to S3 before returning the 404

### Solution

This will involve pausing the consumption of new messages from Common Platform, which will delay hearings from appearing on PACFS.

We are able to retrieve the path to the latest message payload for each hearing affected with [this AppInsights query](https://portal.azure.com#@747381f4-e81f-4a43-bf68-ced6a1e14edf/blade/Microsoft_OperationsManagementSuite_Workspace/Logs.ReactView/resourceId/%2Fsubscriptions%2Fa5ddf257-3b21-4ba9-a28c-ab30f751b383%2FresourceGroups%2Fnomisapi-prod-rg%2Fproviders%2FMicrosoft.Insights%2Fcomponents%2Fnomisapi-prod/source/LogsBlade.AnalyticsShareLinkToQuery/q/H4sIAAAAAAAAA22QTU%252FDMAyG7%252FwKq5d%252BqKMg7YTUExLaDgMEuyFUhcZsGU1SHHcfiB9P2m5dQeQW%252B30f2y%252FhZ4OO3cU37NZICGVlG1k82QrvhUbIcwhK2xBP1ihImdUEt2h4Qlii2iIFg5HQNRXfWtmZplfToWNrJMHKmmJAPj48LyFLktaOe0Yj4cifS8iBSenCF6MwC1Ng67htRa6uFEcNVSmER3kWxi%252FXr3HsOZYkErwdgJX2Jwld%252B2JNdoMln%252BkpdP5Bk47Wm0vvcI3WXvuFIGhVaLGPRtokbgcMMC%252FfWGXgQxmZK2P8fCZRogNr%252FnL7LDzJiRWC5xG7neI1BHeq8rnX2SiMd18yXVjnLAJwYovSxwGXSZD2jY4T9QhfO%252BK7PE4oS1owo1yqjtf%252FCykY28NG1wUH%252F7LFIpMSZrMbrW%252BcC%252BL%252FQzwtmP7m%252FwBka4WlTwIAAA%253D%253D/timespan/2024-09-19T14%3A00%3A55.000Z%2F2024-09-30T16%3A31%3A55.000Z)

Text of query (note especially datetime formatting to remove MS):
```requests
| where cloud_RoleName == "court-hearing-event-receiver"
| where resultCode == 404
| where operation_Name == "POST /**"
| extend hearingId = trim_end('/', tostring(split(url, 'hearing/')[1]))
| order by timestamp
| project hearingId, url, timestamp, operation_Id
| summarize arg_max(timestamp, *) by hearingId
| join kind=inner traces on operation_Id
| where message startswith "File cp/"
| extend filename = trim_end(" saved to .*", trim_start("File ", message))
| extend formattedTime = format_datetime(timestamp,"yyyy-MM-dd HH:mm:ss")
| project hearingId, filename, formattedTime```


To note, AppInsights Azure logs have a limit of 30,000 results so the above query is a sample size of the issue.

// TODO check query is up-to-date

Export the results of this query to CSV, making sure to:
- take into account the 30,000 results limit.
- remove the column names from the first row of the CSV.
- remove any quotes

Then write an endpoint on `court-case-matcher`. 

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

`curl -X POST -F file=@src/main/resources/replay404hearings/hearings.csv http://localhost:8080/replay404Hearings`

## Dry run mode

In dry run mode, the hearing will be retrieved from S3 and the last modified date will be checked against the datetime the hearing was written to S3. The hearing will not be saved.
Log statements and telemetry will indicate if the hearing would have been updated, created or discarded as a more recent version is in the database.


### modifying environment variables on running instances

```
kubectl set env deployment/court-case-matcher REPLAY404_DRY_RUN=false -n namespace
```

### Reporting on the replay

```
customEvents
| where cloud_RoleName == 'court-case-matcher'
| where name == 'PiC404HearingEventProcessed'
| where tostring(customDimensions.dryRun) == 'false'
| summarize count() by tostring(customDimensions.status)
```

This will summarise all events which have been processed by whether they have succeeded, failed or been ignored as we have a more recent version. We will need to do something about the failures

TODO

Omit hearings with no prosecutionCases. Retry in preprod until we have no failures.

There are a lot of problems in preprod - DLQ clear? Fix errors?

Error handling around S3 call?
Build in a retry mechanism? Might not be necessary in production

What else is needed for go/no-go?
Manual database snapshot
Does taking a snapshot interfere with a running process/thread?
