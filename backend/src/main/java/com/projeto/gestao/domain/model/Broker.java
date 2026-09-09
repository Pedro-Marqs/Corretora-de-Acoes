package com.projeto.gestao.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "broker", uniqueConstraints = @UniqueConstraint(name = "uq_broker_cnpj", columnNames = "cnpj"))
public class Broker {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 14) private String cnpj;
    @Column(name = "corporate_name", nullable = false, length = 200) private String corporateName;
    @Column(name = "trade_name", nullable = false, length = 200) private String tradeName;
    @Column(name = "registration_status", nullable = false, length = 40) private String registrationStatus;
    @Column(name = "cvm_category", nullable = false, length = 20) private String cvmCategory;
    @Column(name = "postal_code", nullable = false, length = 8) private String postalCode;
    @Column(nullable = false, length = 200) private String street;
    @Column(nullable = false, length = 30) private String number;
    @Column(length = 100) private String complement;
    @Column(nullable = false, length = 100) private String district;
    @Column(nullable = false, length = 100) private String city;
    @Column(nullable = false, length = 2) private String state;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected Broker() { }

    public static Broker create(UUID id, String cnpj, String corporateName, String tradeName,
            String registrationStatus, String cvmCategory, String postalCode, String street,
            String number, String complement, String district, String city, String state,
            OffsetDateTime updatedAt) {
        Broker broker = new Broker();
        broker.id = require(id, "id");
        broker.cnpj = required(cnpj, "cnpj");
        broker.corporateName = required(corporateName, "corporateName");
        broker.tradeName = required(tradeName, "tradeName");
        broker.registrationStatus = required(registrationStatus, "registrationStatus");
        broker.cvmCategory = required(cvmCategory, "cvmCategory");
        broker.postalCode = required(postalCode, "postalCode");
        broker.street = required(street, "street");
        broker.number = required(number, "number");
        broker.complement = optional(complement);
        broker.district = required(district, "district");
        broker.city = required(city, "city");
        broker.state = required(state, "state");
        broker.updatedAt = require(updatedAt, "updatedAt");
        return broker;
    }

    public void mergeInstitutionalData(String corporateName, String tradeName,
            String registrationStatus, String cvmCategory, String postalCode, String street,
            String number, String complement, String district, String city, String state,
            OffsetDateTime occurredAt) {
        String mergedCorporateName = presentOrCurrent(corporateName, this.corporateName);
        String mergedTradeName = presentOrCurrent(tradeName, this.tradeName);
        String mergedRegistrationStatus = presentOrCurrent(registrationStatus, this.registrationStatus);
        String mergedCvmCategory = presentOrCurrent(cvmCategory, this.cvmCategory);
        String mergedPostalCode = presentOrCurrent(postalCode, this.postalCode);
        String mergedStreet = presentOrCurrent(street, this.street);
        String mergedNumber = presentOrCurrent(number, this.number);
        String mergedComplement = presentOrCurrent(complement, this.complement);
        String mergedDistrict = presentOrCurrent(district, this.district);
        String mergedCity = presentOrCurrent(city, this.city);
        String mergedState = presentOrCurrent(state, this.state);
        boolean changed = !Objects.equals(this.corporateName, mergedCorporateName)
                || !Objects.equals(this.tradeName, mergedTradeName)
                || !Objects.equals(this.registrationStatus, mergedRegistrationStatus)
                || !Objects.equals(this.cvmCategory, mergedCvmCategory)
                || !Objects.equals(this.postalCode, mergedPostalCode)
                || !Objects.equals(this.street, mergedStreet)
                || !Objects.equals(this.number, mergedNumber)
                || !Objects.equals(this.complement, mergedComplement)
                || !Objects.equals(this.district, mergedDistrict)
                || !Objects.equals(this.city, mergedCity)
                || !Objects.equals(this.state, mergedState);
        if (changed) {
            this.corporateName = mergedCorporateName;
            this.tradeName = mergedTradeName;
            this.registrationStatus = mergedRegistrationStatus;
            this.cvmCategory = mergedCvmCategory;
            this.postalCode = mergedPostalCode;
            this.street = mergedStreet;
            this.number = mergedNumber;
            this.complement = mergedComplement;
            this.district = mergedDistrict;
            this.city = mergedCity;
            this.state = mergedState;
            this.updatedAt = require(occurredAt, "occurredAt");
        }
    }

    private static String presentOrCurrent(String value, String current) {
        return value == null || value.isBlank() ? current : value.trim();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    public UUID getId() { return id; }
    public String getCnpj() { return cnpj; }
    public String getCorporateName() { return corporateName; }
    public String getTradeName() { return tradeName; }
    public String getRegistrationStatus() { return registrationStatus; }
    public String getCvmCategory() { return cvmCategory; }
    public String getPostalCode() { return postalCode; }
    public String getStreet() { return street; }
    public String getNumber() { return number; }
    public String getComplement() { return complement; }
    public String getDistrict() { return district; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
