package com.github.eitraz.tre;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eitraz.tre.model.SharedData;
import com.github.eitraz.tre.model.SubscriptionData;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Setter
public class TreApi {
    public static final String SHARED_DATA_QUERY = "[{\"operationName\":\"getSharedData\",\"variables\":{},\"query\":\"query getSharedData {\\n  my3Services {\\n    sharedData {\\n      isUnlimited\\n      sharedFreeUnit {\\n        remainingUnitKey\\n        remainingValue\\n        totalUnitKey\\n        totalValue\\n        __typename\\n      }\\n      sharesWith\\n      subscriptions {\\n        subscription {\\n          balance {\\n            availableCredit\\n            __typename\\n          }\\n          endUser {\\n            firstName\\n            lastName\\n            __typename\\n          }\\n          id\\n          msisdn\\n          status\\n          tariff {\\n            extraUser\\n            id\\n            name\\n            tariffType\\n            unlimited {\\n              data\\n              __typename\\n            }\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}]";
    public static final String TOTAL_DATA_USAGE_QUERY = "[{\"operationName\":\"getTotalDataUsage\",\"variables\":{\"month\":\"[[MONTH]]\",\"subscriptionId\":\"[[SUBSCRIPTION_ID]]\"},\"query\":\"query getTotalDataUsage($month: String, $subscriptionId: String!) {\\n  my3Subscription(subscriptionId: $subscriptionId) {\\n    id\\n    usage(month: $month) {\\n      dataUsage {\\n        totalCost\\n        totalUsageAmount\\n        totalUsageUnit\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}]";

    private final String AUTH_URL = "https://www.tre.se/t/api/graphql";

    @Value("${tre-api.cookie}")
    private String cookie;

    public Mono<JsonNode> queryGraphQL(@NonNull String query) {
        return WebClient.create(AUTH_URL)
                .post()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.COOKIE, cookie)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(query)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    private Double toGb(double value, String unit) {
        // GB
        if (StringUtils.equalsIgnoreCase("gb", unit)) {
            return value;
        }
        // MB
        else if (StringUtils.equalsIgnoreCase("mb", unit)) {
            return value / 1000;
        }
        // B
        else if (StringUtils.equalsIgnoreCase("mb", unit)) {
            return value / 1000 / 1000;
        }
        // 0 as default
        else {
            return 0d;
        }
    }

    public Mono<SharedData> getSharedData() {
        return queryGraphQL(SHARED_DATA_QUERY)
                .flatMap(json -> {
                    JsonNode sharedData = json
                            .get(0)
                            .get("data")
                            .get("my3Services")
                            .get("sharedData")
                            .get(0);

                    JsonNode sharedFreeUnit = sharedData.get("sharedFreeUnit");

                    double remaining = toGb(
                            sharedFreeUnit.get("remainingValue").asDouble(),
                            sharedFreeUnit.get("remainingUnitKey").asText());

                    double total = toGb(
                            sharedFreeUnit.get("totalValue").asDouble(),
                            sharedFreeUnit.get("totalUnitKey").asText());

                    double used = Math.round(((total - remaining) * 1000)) / 1000d;

                    List<Mono<SubscriptionData>> subscriptions = new ArrayList<>();

                    JsonNode subscriptionsNode = sharedData.get("subscriptions");
                    for (JsonNode subscription : subscriptionsNode) {
                        JsonNode subscriptionNode = subscription.get("subscription");
                        String subscriptionId = subscriptionNode.get("id").asText();
                        String name = subscriptionNode.get("endUser").get("firstName").asText();

                        // Add subscription
                        subscriptions.add(getSubscriptionDataUsed(subscriptionId, name));
                    }

                    return Flux.concat(subscriptions)
                            .collectList()
                            .map(subscriptionData -> new SharedData(
                                    total,
                                    remaining,
                                    used,
                                    subscriptionData
                            ));
                });
    }

    private Mono<SubscriptionData> getSubscriptionDataUsed(@NonNull String subscriptionId,
                                                           @NonNull String name) {
        return queryGraphQL(TOTAL_DATA_USAGE_QUERY
                .replace("[[SUBSCRIPTION_ID]]", subscriptionId)
                .replace("[[MONTH]]", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))))
                .map(json -> {
                    JsonNode dataUsage = json
                            .get(0)
                            .get("data")
                            .get("my3Subscription")
                            .get("usage")
                            .get("dataUsage");

                    return new SubscriptionData(
                            name,
                            toGb(
                                    dataUsage.get("totalUsageAmount").asDouble(),
                                    dataUsage.get("totalUsageUnit").asText())
                    );
                });
    }
}
