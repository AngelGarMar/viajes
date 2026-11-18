package com.transporte.viajes.repositories;

import com.transporte.viajes.entities.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoRepository extends JpaRepository<Tipo, Integer> {
}
