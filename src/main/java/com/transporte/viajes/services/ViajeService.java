package com.transporte.viajes.services;

import com.transporte.viajes.dto.ViajeRequest;
import com.transporte.viajes.models.ResponseService;

public interface ViajeService {
    ResponseService createViaje(ViajeRequest request);
    ResponseService getAllViajes();
    ResponseService getAllViajesCreados();
    ResponseService getAllViajesConfirmados();
    ResponseService getAllViajesCancelados();
}
