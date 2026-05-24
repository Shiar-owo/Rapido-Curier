package com.rapidocurier.clientsservice.infrastructure.adapter.out.reniec.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "reniec", url = "${reniec.api.url:http://localhost:9999}")
public interface ReniecFeignClient {

    @GetMapping
    ReniecApiResponse consultarDni(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("numero") String numero
    );

    record ReniecApiResponse(
        @JsonProperty("first_name") String firstName,
        @JsonProperty("first_last_name") String firstLastName,
        @JsonProperty("second_last_name") String secondLastName,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("document_number") String documentNumber
    ) {}
}
