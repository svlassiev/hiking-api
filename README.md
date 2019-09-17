# hiking-api

## Updating
Update version in `gradle.build` and then
```shell script
$ ./gradlew clean build
$ docker build -t svlassiev/hiking-api:<version> .
$ docker push svlassiev/hiking-api:<version>
$ kubectl apply -f k8s/hiking-api.yml
```

## Installing mongo
```shell script
$ kubectl apply -f k8s/mongo.yml
```
