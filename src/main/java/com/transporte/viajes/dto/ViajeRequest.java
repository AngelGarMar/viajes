package com.transporte.viajes.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ViajeRequest implements Serializable {
    @NotNull(message = "El campo origenId no debe estar vacío.")
    private Integer origenId;
    @NotNull(message = "El campo destinoId no debe estar vacío.")
    private Integer destinoId;
    @NotNull(message = "El campo lugaresAdulto no debe estar vacío.")
    @Min(value = 0, message = "El valor debe ser mayor o igual a 0.")
    private Integer lugaresAdulto;
    @NotNull(message = "El campo lugaresMenor no debe estar vacío.")
    @Min(value = 0, message = "El valor debe ser mayor o igual a 0.")
    private Integer lugaresMenor;
    @NotNull(message = "El campo lugaresPaqueteria no debe estar vacío.")
    @Min(value = 0, message = "El valor debe ser mayor o igual a 0.")
    private Integer lugaresPaqueteria;
    @NotNull(message = "El campo lugaresAdultoMayor no debe estar vacío.")
    @Min(value = 0, message = "El valor debe ser mayor o igual a 0.")
    private Integer lugaresAdultoMayor;
    @NotNull(message = "El campo lugaresCortesia no debe estar vacío.")
    @Min(value = 0, message = "El valor debe ser mayor o igual a 0.")
    private Integer lugaresCortesia;
    @NotNull(message = "El campo usuarioId no debe estar vacío.")
    private Integer usuarioId;
    @NotNull(message = "El campo clienteId no debe estar vacío.")
    private Integer clienteId;
    @NotNull(message = "El campo conductorId no debe estar vacío.")
    private Integer conductorId;
}
