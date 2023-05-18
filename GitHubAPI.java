// camel-k: language=java dependency=camel:netty-http dependency=camel:jackson property=file:application.properties

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.http.NettyHttpMessage;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;

public class GitHubAPI extends RouteBuilder {

    @Override
    public void configure() throws Exception {  
        rest("/dummy")
            .post()
                .consumes("application/json").produces("application/json")
            .to("direct:postToGithub");
    
        from("direct:postToGithub")
            .log(LoggingLevel.DEBUG, "com.github.validation.GitHubDummy", "Logging payload: ${body} and headers: (${header[x-my-header]} - ${header[authorization]})")
            .setProperty("repositories", simple("${body}"))
            .log(LoggingLevel.DEBUG, "com.github.validation.GitHubDummy", "Property: ${exchangeProperty.repositories}")
            .process(exchange -> {
                var body = Map.of(
                    "username", exchange.getContext().resolvePropertyPlaceholders("{{dummy.username}}"), 
                    "password", exchange.getContext().resolvePropertyPlaceholders("{{dummy.password}}")
                );
                exchange.getIn().setBody(body);
            })
            .removeHeaders(Exchange.HTTP_PATH) // see https://github.com/apache/camel-k/issues/2867#issuecomment-1032608532
            .removeHeader("authorization")
            .split(simple("${exchangeProperty.repositories}"), new GitHubStrategy()).streaming()
                .setHeader("organization", simple("${body[organization]}"))
                .setHeader("repository", simple("${body[repository]}"))
                .setBody(simple("${null}"))
                .to("rest://get:{{github.issue-path}}?host={{github.api.base-url}}&routeId=githubIssues")
                .process(this::debug)
                .unmarshal().json(JsonLibrary.Jackson)
            .end()
            .to("log:com.github.validation.GitHubDummy?level=INFO");
    }

    private void debug(Exchange exchange) {
        NettyHttpMessage message = exchange.getIn(NettyHttpMessage.class);
        log.info("message {}", message);
    }

    public static class GitHubStrategy implements AggregationStrategy {
        private static final Logger log = org.slf4j.LoggerFactory.getLogger(GitHubStrategy.class);

        @Override
        @SuppressWarnings({"rawtypes"})
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            List<List<Object>> results = new ArrayList<>();
            if(oldExchange != null) {
                Throwable oldCause = oldExchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
                if(oldCause == null)
                    results = oldExchange.getIn().getBody(List.class);
                log.info("Results: {}", results);
            } else {
                var result = newExchange.getIn().getBody(List.class);
                results.add(result);
                newExchange.getIn().setBody(results);
                return newExchange;
            }

            Throwable newCause = newExchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
            if(newCause == null) {
                var result = newExchange.getIn().getBody(List.class);
                results.add(result);
                oldExchange.getIn().setBody(results);
            }
            return oldExchange;
        }
    }
}
