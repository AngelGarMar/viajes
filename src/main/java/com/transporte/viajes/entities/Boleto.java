package com.transporte.viajes.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "boletos")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Boleto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    private Integer numero;
    private Timestamp creado;
    private Timestamp modificado;
    private Integer estatus;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viajes_id")
    private Viaje viaje;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipos_id")
    private Tipo tipo;
}
