package com.projeto.gestao.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "broker")
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
