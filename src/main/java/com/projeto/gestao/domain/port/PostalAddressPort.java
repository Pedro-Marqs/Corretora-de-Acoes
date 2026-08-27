package com.projeto.gestao.domain.port;

import com.projeto.gestao.domain.model.PostalAddress;

public interface PostalAddressPort {
    PostalAddress findByPostalCode(String postalCode);
}
