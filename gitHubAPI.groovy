// camel-k: language=groovy dependency=camel:jackson dependency=camel:netty-http property=file:application.properties

rest {
    path('/dummy') {
        post {
            consumes 'application/json'
            produces 'application/json'
            to 'direct:postToGithub'
        }
    }
}
  
from('direct:postToGithub')
    .log(org.apache.camel.LoggingLevel.DEBUG, 'com.github.validation.GitHubDummy', 'Logging payload ${body} and headers: ${header[x-country-code]} - ${header[authorization]}')
    .setProperty('repositories', simple('${body}'))
    .log(org.apache.camel.LoggingLevel.DEBUG, 'com.github.validation.GitHubDummy', 'Property: ${exchangeProperty.repositories}')
    .process { 
        it.in.body = [
            username: it.context.resolvePropertyPlaceholders('{{dummy.username}}'), 
            password: it.context.resolvePropertyPlaceholders('{{dummy.password}}')
        ] 
    }
    .removeHeaders('CamelHttpPath') // see https://github.com/apache/camel-k/issues/2867#issuecomment-1032608532
    .removeHeader('authorization')
    .split(simple('${exchangeProperty.repositories}')).streaming()
        .setHeader('organization', simple('${body[organization]}'))
        .setHeader('repository', simple('${body[repository]}'))
        .setBody(simple('${null}'))
        .to('rest://get:{{github.issue-path}}?host={{github.api.base-url}}&routeId=githubIssues')
        .process {
            log('message ==> ' + it.in)
        }
        .unmarshal().json(org.apache.camel.model.dataformat.JsonLibrary.Jackson)
    .end()
    .to("log:com.github.validation.GitHubDummy?level=INFO")
