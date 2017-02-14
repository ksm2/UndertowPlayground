# UndertowPlayground
An example project of Undertow using http2 with SSL and Jackson

## Getting started

To build the project run
```bash
mvn clean package
```

Then build a container with docker
```bash
docker build -t undertow .
```

Create a docker network “undertowtest”
```bash
docker network create undertowtest
```

Start a MongoDB in “undertowtest”
```bash
docker run -d --name mongo --network-alias mongo --net undertowtest mongo
```

Start the Undertow server in “undertowtest”
```bash
docker run -d -p 443:8443 --name undertow --network-alias undertow --net undertowtest  undertow
```

Add the SSL certificate to your trusted certificates.
On Arch Linux you do that by running
```bash
cp src/main/resources/server.crt /etc/ca-certificates/trust-source/anchors/undertow.crt
trust expose-compat
```

Proceed to [/pet](https://localhost/pet) in your favourite Browser!

## Test RESTful API with cURL

```bash
# Post a new pet called “herby”
curl --cacert src/main/resources/server.crt -X POST -d '{"name":"herby","age":42}' https://localhost/pet
# Get Herby
curl --cacert src/main/resources/server.crt -X GET https://localhost/pet/herby
# Update Herby
curl --cacert src/main/resources/server.crt -X PUT -d '{"name":"herby","age":23}' https://localhost/pet/herby
# Increase Herby's age
curl --cacert src/main/resources/server.crt -X POST https://localhost/pet/herby/increaseAge
# Remove Herby
curl --cacert src/main/resources/server.crt -X DELETE https://localhost/pet/herby
```

## Stop it

```bash
docker rm -f undertow
```
