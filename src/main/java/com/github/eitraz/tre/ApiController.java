package com.github.eitraz.tre;

import com.github.eitraz.tre.model.SharedData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final TreApi api;

    @GetMapping(value = "/data-usage", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<SharedData>> getDataUsage() {
        return api.getSharedData()
                .map(sharedData -> ResponseEntity
                        .ok()
                        .body(sharedData))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
