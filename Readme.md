# Camel K integration for GitHub

> moved to <[https://github.com/juliuskrah/experiments/camel-k-examples](https://github.com/juliuskrah/experiments/tree/main/quarkus-native-demo)>

Run the application in dev mode

<details>
  <summary>gitHubAPI.groovy</summary>
  
```bash
kamel run --dependency camel-jackson --dependency camel-netty-http --property file:application.properties --trait service.node-port=false --trait logging.level=DEBUG gitHubAPI.groovy --dev
```
</details>

<details>
  <summary>GitHubAPI.java</summary>
  
```bash
kamel run --dependency camel-jackson --dependency camel-netty-http --property file:application.properties --trait service.node-port=false --trait logging.level=DEBUG GitHubAPI.java --dev
```
</details>


Forward the port

```bash
kubectl port-forward services/git-hub-api 8080:80
```

# Payload

```bash
curl --location 'http://localhost:8080/dummy' \
--header 'x-my-header: foo' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer foobar' \
--data '[
    {
        "organization": "juliuskrah",
        "repository": "quarkus-liquibase"
    },
    {
        "organization": "juliuskrah",
        "repository": "hibernate-liquibase"
    },
    {
        "organization": "spring-projects",
        "repository": "spring-boot"
    }
]'
```

## Configuring Camel-K Operator

run `kubectl edit ip camel-k -n development`  change from Java 11 to Java 17

```yaml
status:
  build:
    baseImage: eclipse-temurin:17-alpine
```

### Maven Repository

settings.xml 

```xml
<settings>
    <profiles>
        <profile>
            <id>camel-k</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <repositories>
                <repository>
                    <id>camel-repo-snapshot</id>
                    <!--...-->
                </repository>
                <repository>
                    <id>camel-repo-release</id>
                    <!--...-->
                </repository>
            </repositories>
        </profile>
    </profiles>
    <servers>
        <server>
            <id>camel-repo-snapshot</id>
            <username>xxx</username>
            <password>xxx</password>
        </server>
        <server>
            <id>camel-repo-release</id>
            <username>xxx</username>
            <password>xxx</password>
        </server>
    </servers>
</settings>
```
