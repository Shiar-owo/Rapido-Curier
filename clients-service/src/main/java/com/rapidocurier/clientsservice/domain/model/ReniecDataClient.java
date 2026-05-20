package com.rapidocurier.clientsservice.domain.model;

public record ReniecDataClient(
    String firstName,
    String firstLastName,
    String secondLastName,
    String fullName,
    String documentNumber
) {}