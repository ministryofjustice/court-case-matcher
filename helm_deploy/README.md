
### Helm init

```
helm init --tiller-namespace court-probation-dev --service-account tiller --history-max 200
```

### Example deploy command
```
helm --namespace court-probation-dev  --tiller-namespace court-probation-dev upgrade court-case-matcher ./court-case-matcher/ --install --values=values-dev.yaml
```

### Rolling back a release
Find the revision number for the deployment you want to roll back:
```
helm -n court-probation-dev history court-case-matcher 
```
(note, each revision has a description which has the app version and circleci build URL)

Rollback
```
helm -n court-probation-dev rollback court-case-matcher [INSERT REVISION NUMBER HERE] --wait
```
### Setup Lets Encrypt cert

Ensure the certificate definition exists in the cloud-platform-environments repo under the relevant namespaces folder

e.g.
```
cloud-platform-environments/namespaces/live-1.cloud-platform.service.justice.gov.uk/[INSERT NAMESPACE NAME]/05-certificate.yaml
```
