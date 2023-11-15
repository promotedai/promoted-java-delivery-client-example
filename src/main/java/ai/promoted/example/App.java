package ai.promoted.example;

import ai.promoted.delivery.client.DeliveryException;
import ai.promoted.delivery.client.DeliveryRequest;
import ai.promoted.delivery.client.DeliveryResponse;
import ai.promoted.delivery.client.PromotedDeliveryClient;
import ai.promoted.delivery.model.Insertion;
import ai.promoted.delivery.model.Paging;
import ai.promoted.delivery.model.Request;
import ai.promoted.delivery.model.UseCase;
import ai.promoted.delivery.model.UserInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;

import java.util.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.PropertiesDefaultProvider;

@Command(name = "App", mixinStandardHelpOptions = true, defaultValueProvider = PropertiesDefaultProvider.class)
public class App implements Callable<Integer>
{
    @Option(names = "--metricsApiEndpointUrl", description = "Metrics API Endpoint URL.  Get this from Promoted.  E.g. https://.../log",
        defaultValue = "")
    private String metricsApiEndpointUrl;

    @Option(names = "--metricsApiKey", description = "Metrics API Key.  Get this from Promoted.",
        defaultValue = "")
    private String metricsApiKey;

    @Option(names = "--deliveryApiEndpointUrl", description = "Delivery API Endpoint URL.  Get this from Promoted.  E.g. https://.../deliver",
        defaultValue = "")
    private String deliveryApiEndpointUrl;

    @Option(names = "--deliveryApiKey", description = "Delivery API Key.  Get this from Promoted.",
        defaultValue = "")
    private String deliveryApiKey;

    @Option(names = "--onlyLog", description = "Indicates whether to onlyLog the call.",
        defaultValue = "true")
    private boolean onlyLog = true;

    @Option(names = "--warmup", description = "Whether to warm up the SDK.",
        defaultValue = "false")
    private boolean warmup;

    @Option(names = "--shadowTrafficDeliveryRate", description = "shadowTrafficDeliveryRate.",
        defaultValue = "0.0")
    private float shadowTrafficDeliveryRate = 0.0f;

    @Option(names = "--blockingShadowTraffic", description = "blockingShadowTraffic.",
        defaultValue = "false")
    private boolean blockingShadowTraffic = false;

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    public Integer call() throws Exception {
        checkArgument(!deliveryApiEndpointUrl.isEmpty(), "deliveryApiEndpointUrl needs to be specified");
        checkArgument(!deliveryApiKey.isEmpty(), "deliveryApiKey needs to be specified");
        checkArgument(!metricsApiEndpointUrl.isEmpty(), "metricsApiEndpointUrl needs to be specified");
        checkArgument(!metricsApiKey.isEmpty(), "metricsApiKey needs to be specified");

        PromotedDeliveryClient client = PromotedDeliveryClient.builder()
            .withExecutor(Executors.newFixedThreadPool(2))
            .withDeliveryEndpoint(deliveryApiEndpointUrl)
            .withDeliveryApiKey(deliveryApiKey)
            .withDeliveryTimeoutMillis(250)
            .withMetricsEndpoint(metricsApiEndpointUrl)
            .withMetricsApiKey(metricsApiKey)
            .withMetricsTimeoutMillis(1000)
            .withWarmup(warmup)
            .withShadowTrafficDeliveryRate(shadowTrafficDeliveryRate)
            .withBlockingShadowTraffic(blockingShadowTraffic)
            .build();

        Request request = newTestRequest();
        DeliveryRequest deliveryRequest = new DeliveryRequest(request, null, onlyLog, 0);
        DeliveryResponse response = client.deliver(deliveryRequest);
        System.out.println("response=" + toJson(response));
        return 0;
    }

    private static Request newTestRequest() {
        Request request = new Request();
        UserInfo userInfo = new UserInfo();
        userInfo.setAnonUserId("anonUserId1");
        request.setUserInfo(userInfo);
        request.setUseCase(UseCase.SEARCH);
        request.setSearchQuery("query");
        request.setPaging(new Paging().offset(0).size(2));
        request.setDisablePersonalization(false); // default=false.
        for (int i = 0; i < 3; i++) {
            Insertion insertion = new Insertion();
            insertion.setContentId("content" + i);
            insertion.setRetrievalRank(i);
            request.addInsertionItem(insertion);
            // TODO - set custom properties.
        }
        return request;
    }

    private static String toJson(Object obj) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(obj);
    }

    private static void checkArgument(boolean value, String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
    }
}
