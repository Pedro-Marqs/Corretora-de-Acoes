package com.projeto.gestao.domain.model;

/** Endereço postal interno normalizado. */
public record PostalAddress(
        String postalCode,
        String street,
        String complement,
        String neighborhood,
        String city,
        String state) {
}
