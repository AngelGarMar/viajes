package com.transporte.viajes.clients;

import com.transporte.viajes.models.ParaderosRequest;
import com.transporte.viajes.models.ResponseService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface ParaderosClient {
    @PostExchange("/paraderos")
    ResponseService existsParaderos(@RequestBody ParaderosRequest request);
}
