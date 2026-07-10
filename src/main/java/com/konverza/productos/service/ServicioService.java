package com.konverza.productos.service;

import com.konverza.productos.dto.ServicioRequest;
import com.konverza.productos.entity.Servicio;
import com.konverza.productos.exception.ServicioNotFoundException;
import com.konverza.productos.repository.ServicioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public List<Servicio> findAll() {
        return servicioRepository.findAll();
    }

    public Servicio findById(UUID id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new ServicioNotFoundException(id));
    }

    public Servicio create(ServicioRequest req) {
        Servicio servicio = Servicio.builder()
                .name(req.getName())
                .description(req.getDescription())
                .context(req.getContext())
                .build();
        return servicioRepository.save(servicio);
    }

    public Servicio update(UUID id, ServicioRequest req) {
        Servicio servicio = findById(id);
        servicio.setName(req.getName());
        servicio.setDescription(req.getDescription());
        servicio.setContext(req.getContext());
        return servicioRepository.save(servicio);
    }

    public void delete(UUID id) {
        Servicio servicio = findById(id);
        servicioRepository.delete(servicio);
    }
}
