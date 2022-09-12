package com.github.eitraz.tre;

import com.github.eitraz.tre.model.SubscriptionData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.CaseUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final TreApi api;

    @GetMapping(value = "/data-usage", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getDataUsage() {
        return api.getSharedData()
                .map(sharedData -> {
                    return Map.of(
                            "total", sharedData.used(),
                            "subscriptions", sharedData.subscriptions().stream()
                                    .collect(Collectors.toMap(
                                            subscriptionData -> formatName(subscriptionData.name()),
                                            SubscriptionData::used
                                    ))
                    );
                });
    }

    private static String formatName(String name) {
        return CaseUtils.toCamelCase(name, true, ' ', '-');
    }
}
