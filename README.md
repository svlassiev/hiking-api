# hiking-api

## Updating
Update version in `gradle.build` and then
```shell script
$ ./gradlew clean build
$ docker build -t svlassiev/hiking-api:<version> .
$ docker push svlassiev/hiking-api:<version>
```
Change version in `hiking-api.yml` at `svlassiev/hiking-api:<version>` string
```shell script
$ kubectl apply -f k8s/hiking-api.yml
```

## Installing mongo
```shell script
$ kubectl apply -f k8s/mongo.yml
```
