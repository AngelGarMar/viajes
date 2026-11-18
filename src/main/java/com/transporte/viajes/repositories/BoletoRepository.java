package com.transporte.viajes.repositories;

import com.transporte.viajes.entities.Boleto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletoRepository extends JpaRepository<Boleto, Integer> {
}
