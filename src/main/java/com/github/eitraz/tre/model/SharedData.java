package com.github.eitraz.tre.model;

import java.util.List;

public record SharedData(Double total,
                         Double remaining,
                         Double used,
                         List<SubscriptionData> subscriptions) {
}
