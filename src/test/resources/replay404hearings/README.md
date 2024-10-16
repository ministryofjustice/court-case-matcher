# PIC-4207 recovering lost data

## Overview of problem and solution

### Problem
All requests to court-case-matcher/hearing/{id}/ returned a 404 between 19 and 30 September because of a Spring Boot upgrade which changed the
default behaviour. Previously requests with a trailing slash were mapped to an endpoint defined without a trailing slash.
Fortunately a filter wrote the message payloads to S3 before returning the 404

### Solution

This will involve pausing the consumption of new messages from Common Platform, which will delay hearings from appearing on PACFS.

We are able to retrieve the path to the latest message payload for each hearing affected with [this AppInsights query](https://portal.azure.com#@747381f4-e81f-4a43-bf68-ced6a1e14edf/blade/Microsoft_OperationsManagementSuite_Workspace/Logs.ReactView/resourceId/%2Fsubscriptions%2Fa5ddf257-3b21-4ba9-a28c-ab30f751b383%2FresourceGroups%2Fnomisapi-prod-rg%2Fproviders%2Fmicrosoft.insights%2Fcomponents%2Fnomisapi-prod/source/LogsBlade.AnalyticsShareLinkToQuery/q/H4sIAAAAAAAAA3WSwU7DMAyG7zyF1cvaqVuH4ITUExKCwwYCbghVWWO2QJMUx90G4uFxu60bCHJK7N%252Bf7b8lfG8wcDj5gvUSCaGsfKOLe1%252FhTFmEPIeo9A3xaImKjFuMcIWOR4QlmhVS1BcShqbiS6%252B7ovPJeZ%252FxNZJi413RI%252B9uHx4hGw7bctwwOg07%252Fo2GHJiMLSQYD7JBCuwDt6k41JXhuKEqhcFOng2Sp9PnJBGOJ40E8w9gY2UlZWsJ1uRfseQDPYWuvtekR%252BPdaKkIjbWi%252FURQtCis2sRH2mHSNuhhIn%252F1xsGbcTo3zkl%252FJlViAO9%252Bc7deCCmoBYLwiMPa8BKiK1OJ73UW%252FbfEzqEX0bnOwYNBEQS1Qi0ewXgYpdtEB4%252B3XIntenYm7VGerGJG%252FWg63vZdaMXYNj5aOdI6m06zDzlwfX1h7UUI0RGJ%252FHrW2LkMnbf3wnWPODn8F71gjrxGdBCfwngMZxM5yd%252BfaL9p%252BnPQbzC2%252BbmtAgAA/timespan/2024-09-19T05%3A57%3A05.000Z%2F2024-09-30T22%3A57%3A05.000Z)

Text of query (note especially datetime formatting to remove MS):
```

requests
| where cloud_RoleName == "court-hearing-event-receiver"
| where resultCode == 404
| where operation_Name == "POST /**"
| extend hearingId = trim_end('/', tostring(split(url, 'hearing/')[1]))
| order by timestamp
| project hearingId, url, timestamp, operation_Id
| summarize arg_max(timestamp, *) by hearingId
| join kind=inner traces on operation_Id
| where message startswith "File cp/"
| order by timestamp
| extend filename = trim_end(" saved to .*", trim_start("File ", message))
| extend formattedTime = format_datetime(timestamp,"dd/MM/yyyy HH:mm:ss")
| extend rowNumber = row_number()
| where rowNumber between (1 .. 30000)
| project hearingId, filename, formattedTime

```


To note, AppInsights Azure logs have a limit of 30,000 results. You can work around this using row_number() as in the above query 
Running [the first part of the query](https://portal.azure.com#@747381f4-e81f-4a43-bf68-ced6a1e14edf/blade/Microsoft_OperationsManagementSuite_Workspace/Logs.ReactView/resourceId/%2Fsubscriptions%2Fa5ddf257-3b21-4ba9-a28c-ab30f751b383%2FresourceGroups%2Fnomisapi-prod-rg%2Fproviders%2Fmicrosoft.insights%2Fcomponents%2Fnomisapi-prod/source/LogsBlade.AnalyticsShareLinkToQuery/q/H4sIAAAAAAAAA1WQPU%252FDMBBAd37FqUvsKFGK1DUTUxda0W4IRSY%252BtUb%252BCOdzaRE%252FHqeqTFnv3nv%252BIPxMGDk%252B%252FMDXEQlhtCHp4SVYfFYOoe9hMYZE3B5RkfGHFk%252FouSUc0ZyQFkUkjMnyU9BXabVclU2YkBSb4IeS3G52e%252BjqetbxzOg13PprDT0wGTfkoai6qgEOkeeViJM1LBLZBqob3lXy9fFNytwJpJHg%252FQJsXH6SclMeThQ%252BcOS%252FegNXvzDN3fXWOhsxOZfZbwRFh8Gps7hjazkfUGL%252F8PxNnoX8BQ27NHRSAQAA/timespan/2024-09-19T05%3A57%3A05.000Z%2F2024-09-30T22%3A57%3A05.000Z), to count the 404s, gives 88582 results (Joining on traces loses 19 for some reason so we end up with 88563)
So the query must be split into three batches, which you can do by adding 30000 to the rowNumbers in the final where clause.

Export the results of this query to CSV, making sure to:
- remove the column names from the first row of the CSV
- remove any quotes
- check that the date format is `dd/MM/yyyy HH:mm:ss` so that it looks like `28/09/2024 16:32:04`

## How to replay hearings

### Overview

- Subscribe `court-case-matcher` to an empty queue to avoid race conditions when processing updates
- Each hearing will be saved to the database unless a more recent version exists
- Once processing has completed, subscribe `court-case-matcher` to the court-case-events queue once more.

### Process

The endpoint `/replay404Hearings` is only available from within the k8s cluster.  
Set up a port-forward from your machine to the `court-case-matcher` application like this: 
`kubectl -n court-probation-preprod port-forward deployments/court-case-matcher 8080:8080`

This will forward `http://localhost:8080` on your machine to one of the `court-case-matcher` pods in the chosen namespace

You can then post a file in the format of [test-hearings.csv](src/test/resources/replay404hearings/test-hearings.csv) like this:

`curl -vv -F file=@src/main/resources/replay404hearings/hearings.csv http://localhost:8080/replay404Hearings`
 
## Dry run mode

In dry run mode, the hearing will be retrieved from S3 and the last modified date will be checked against the datetime the hearing was written to S3. The hearing will not be saved.
Log statements and telemetry will indicate if the hearing would have been updated, created or discarded as a more recent version is in the database.

### Modifying environment variables on running instances

#### Disabling Dry Run mode

```
kubectl set env deployment/court-case-matcher REPLAY404_DRY_RUN=false -n namespace
```

Note that this will restart all pods in the deployment

#### Enabling Dry Run mode

```
kubectl set env deployment/court-case-matcher REPLAY404_DRY_RUN=true -n namespace
```

### Reporting on the replay

Run the following AppInsights query - amend the line `| where tostring(customDimensions.dryRun) == 'false'` as applicable

```
customEvents
| where cloud_RoleName == 'court-case-matcher'
| where name == 'PiC404HearingEventProcessed'
| summarize count() by tostring(customDimensions.status)
```

This will summarise all events which have been processed by whether they have succeeded, failed or been ignored as we have a more recent version or cannot process them (e.g. they have no prosectionCases). We will need to do something about the failures

TODO

Omit hearings with no prosecutionCases. Retry in preprod until we have no failures.

There are a lot of problems in preprod - DLQ clear? Fix errors?

Error handling around S3 call?
Build in a retry mechanism? Might not be necessary in production

What else is needed for go/no-go?
Manual database snapshot
Does taking a snapshot interfere with a running process/thread?
