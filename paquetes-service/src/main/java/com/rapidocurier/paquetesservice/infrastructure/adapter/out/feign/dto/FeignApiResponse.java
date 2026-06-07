package com.rapidocurier.paquetesservice.infrastructure.adapter.out.feign.dto;

public record FeignApiResponse<T>(
    boolean success,
    String message,
    T data
) {}
