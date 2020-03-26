# hiking-api

## Updating
Update version in `build.gradle` and `hiking-api.yml` at `svlassiev/hiking-api:<version>` string. Then create tag and push. Dockerhub will be triggered by tag commit and build new docker image.

Just apply new changes.
```shell script
$ kubectl apply -f k8s/hiking-api.yml
```

## Installing mongo
```shell script
$ kubectl apply -f k8s/mongo.yml
```
