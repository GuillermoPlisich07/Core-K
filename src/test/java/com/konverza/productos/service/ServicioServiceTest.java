package com.konverza.productos.service;

import com.konverza.productos.dto.ServicioRequest;
import com.konverza.productos.entity.Servicio;
import com.konverza.productos.exception.ServicioNotFoundException;
import com.konverza.productos.repository.ServicioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock private ServicioRepository servicioRepository;

    private ServicioService servicioService;

    @BeforeEach
    void setUp() {
        servicioService = new ServicioService(servicioRepository);
    }

    private ServicioRequest request(String name) {
        ServicioRequest req = new ServicioRequest();
        req.setName(name);
        req.setDescription("desc");
        req.setContext("ctx");
        return req;
    }

    @Test
    @DisplayName("findAll returns every servicio")
    void findAll_returnsAll() {
        when(servicioRepository.findAll()).thenReturn(List.of(Servicio.builder().id(UUID.randomUUID()).name("A").build()));
        assertThat(servicioService.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("create persists a new servicio with the submitted fields")
    void create_persistsFields() {
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        Servicio created = servicioService.create(request("Consultoría"));

        assertThat(created.getName()).isEqualTo("Consultoría");
        assertThat(created.getDescription()).isEqualTo("desc");
        assertThat(created.getContext()).isEqualTo("ctx");
    }

    @Test
    @DisplayName("update changes the fields of an existing servicio")
    void update_changesFields() {
        UUID id = UUID.randomUUID();
        Servicio existing = Servicio.builder().id(id).name("Old").build();
        when(servicioRepository.findById(id)).thenReturn(Optional.of(existing));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        Servicio updated = servicioService.update(id, request("New"));

        assertThat(updated.getName()).isEqualTo("New");
    }

    @Test
    @DisplayName("update throws for a nonexistent servicio")
    void update_missingServicio_throws() {
        UUID id = UUID.randomUUID();
        when(servicioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicioService.update(id, request("New")))
                .isInstanceOf(ServicioNotFoundException.class);
    }

    @Test
    @DisplayName("delete removes an existing servicio")
    void delete_existingServicio_removesIt() {
        UUID id = UUID.randomUUID();
        Servicio existing = Servicio.builder().id(id).name("X").build();
        when(servicioRepository.findById(id)).thenReturn(Optional.of(existing));

        servicioService.delete(id);

        verify(servicioRepository).delete(existing);
    }

    @Test
    @DisplayName("delete throws for a nonexistent servicio instead of silently no-op'ing")
    void delete_missingServicio_throws() {
        UUID id = UUID.randomUUID();
        when(servicioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicioService.delete(id))
                .isInstanceOf(ServicioNotFoundException.class);
        verify(servicioRepository, never()).delete(any());
    }
}
