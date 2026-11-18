package com.transporte.viajes.clients;

import com.transporte.viajes.models.ResponseService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface GetParaderoClient {
    @GetExchange("/paraderos/{idParadero}")
    ResponseService getParadero(@PathVariable Integer idParadero);
}
