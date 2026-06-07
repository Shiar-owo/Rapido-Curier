package com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data
) {}
