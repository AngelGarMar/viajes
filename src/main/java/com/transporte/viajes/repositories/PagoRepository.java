package com.transporte.viajes.repositories;

import com.transporte.viajes.entities.Pago;
import com.transporte.viajes.entities.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {
    Optional<Pago> getByViaje(Viaje viaje);
}
