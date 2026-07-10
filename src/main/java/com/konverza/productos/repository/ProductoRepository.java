package com.konverza.productos.repository;

import com.konverza.productos.entity.Producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, UUID> {
}
