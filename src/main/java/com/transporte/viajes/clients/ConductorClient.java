package com.transporte.viajes.clients;

import com.transporte.viajes.models.ResponseService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface ConductorClient {
    @GetExchange("/drivers/{driverId}")
    ResponseService getConductor(@PathVariable Integer driverId);
}
