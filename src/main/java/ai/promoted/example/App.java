package ai.promoted.example;

import ai.promoted.delivery.client.DeliveryRequest;
import ai.promoted.delivery.client.DeliveryResponse;
import ai.promoted.delivery.client.PromotedDeliveryClient;
import ai.promoted.proto.common.UserInfo;
import ai.promoted.proto.delivery.Insertion;
import ai.promoted.proto.delivery.Paging;
import ai.promoted.proto.delivery.Request;
import ai.promoted.proto.delivery.UseCase;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;

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

    @Option(names = "--useGrpc", description = "Whether to use gRPC for delivery calls.",
        defaultValue = "true")
    private boolean useGrpc;

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
            .withUseGrpc(useGrpc)
            .build();

        Request.Builder requestBuilder = newTestRequestBuilder();
        DeliveryRequest deliveryRequest = new DeliveryRequest(requestBuilder, null, onlyLog, 0);
        DeliveryResponse response = client.deliver(deliveryRequest);
        System.out.println("execution server=" + response.getExecutionServer());
        System.out.println("client request ID=" + response.getClientRequestId());
        System.out.println("response=" + response.getResponse());
        return 0;
    }

    private static Request.Builder newTestRequestBuilder() {
        Request.Builder requestBuilder = Request.newBuilder();
        UserInfo.Builder userInfoBuilder = requestBuilder.getUserInfoBuilder();
        userInfoBuilder.setAnonUserId("anonUserId1");
        requestBuilder.setUseCase(UseCase.SEARCH);
        requestBuilder.setSearchQuery("query");
        requestBuilder.setPaging(Paging.newBuilder().setOffset(0).setSize(2));
        requestBuilder.setDisablePersonalization(false); // default=false.
        for (int i = 0; i < 3; i++) {
            Insertion.Builder insertionBuilder = requestBuilder.addInsertionBuilder();
            insertionBuilder.setContentId("content" + i);
            insertionBuilder.setRetrievalRank(i);
            // TODO - set custom properties.
        }
        return requestBuilder;
    }

    private static void checkArgument(boolean value, String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
    }
}
