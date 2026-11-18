package com.transporte.viajes.repositories;

import com.transporte.viajes.entities.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViajeRepository extends JpaRepository<Viaje, Integer> {
}
