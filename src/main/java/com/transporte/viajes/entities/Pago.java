package com.transporte.viajes.entities;

import com.transporte.viajes.enums.PagoStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "pagos")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Enumerated(EnumType.STRING)
    private PagoStatus estatus;
    private Timestamp creado;
    @NotNull
    private double costo;
    private Timestamp modificado;
    @OneToOne
    @JoinColumn(name = "viajes_id")
    private Viaje viaje;
}
