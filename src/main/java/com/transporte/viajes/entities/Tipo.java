package com.transporte.viajes.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "tipos")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tipo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotBlank
    @Size(min = 1, max = 45)
    private String descripcion;
    private Timestamp creado;
    private Timestamp modificado;
    @NotNull
    private Integer estatus;
    @NotNull
    private Float costo;
    @OneToMany(mappedBy = "tipo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Boleto> boletos;
}
