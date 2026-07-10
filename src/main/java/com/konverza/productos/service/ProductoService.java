package com.konverza.productos.service;

import com.konverza.productos.dto.ProductoRequest;
import com.konverza.productos.entity.Producto;
import com.konverza.productos.exception.ProductoNotFoundException;
import com.konverza.productos.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public List<Producto> findAll() {
        return productoRepository.findAll();
    }

    public Producto findById(UUID id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
    }

    public Producto create(ProductoRequest req) {
        Producto producto = Producto.builder()
                .name(req.getName())
                .description(req.getDescription())
                .context(req.getContext())
                .build();
        return productoRepository.save(producto);
    }

    public Producto update(UUID id, ProductoRequest req) {
        Producto producto = findById(id);
        producto.setName(req.getName());
        producto.setDescription(req.getDescription());
        producto.setContext(req.getContext());
        return productoRepository.save(producto);
    }

    public void delete(UUID id) {
        Producto producto = findById(id);
        productoRepository.delete(producto);
    }
}
