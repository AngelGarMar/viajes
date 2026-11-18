package com.transporte.viajes.dto;

import com.transporte.viajes.enums.PagoStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class ViajeResponse implements Serializable {
    private Integer id;
    private String origen;
    private String destino;
    private int lugares;
    private String creado;
    private String cliente;
    private String conductor;
    private PagoStatus estatus;
    private double costo;
}
