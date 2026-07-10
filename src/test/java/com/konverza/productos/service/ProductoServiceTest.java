package com.konverza.productos.service;

import com.konverza.productos.dto.ProductoRequest;
import com.konverza.productos.entity.Producto;
import com.konverza.productos.exception.ProductoNotFoundException;
import com.konverza.productos.repository.ProductoRepository;

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
class ProductoServiceTest {

    @Mock private ProductoRepository productoRepository;

    private ProductoService productoService;

    @BeforeEach
    void setUp() {
        productoService = new ProductoService(productoRepository);
    }

    private ProductoRequest request(String name) {
        ProductoRequest req = new ProductoRequest();
        req.setName(name);
        req.setDescription("desc");
        req.setContext("ctx");
        return req;
    }

    @Test
    @DisplayName("findAll returns every producto")
    void findAll_returnsAll() {
        when(productoRepository.findAll()).thenReturn(List.of(Producto.builder().id(UUID.randomUUID()).name("A").build()));
        assertThat(productoService.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("create persists a new producto with the submitted fields")
    void create_persistsFields() {
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto created = productoService.create(request("CRM"));

        assertThat(created.getName()).isEqualTo("CRM");
        assertThat(created.getDescription()).isEqualTo("desc");
        assertThat(created.getContext()).isEqualTo("ctx");
    }

    @Test
    @DisplayName("update changes the fields of an existing producto")
    void update_changesFields() {
        UUID id = UUID.randomUUID();
        Producto existing = Producto.builder().id(id).name("Old").build();
        when(productoRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto updated = productoService.update(id, request("New"));

        assertThat(updated.getName()).isEqualTo("New");
    }

    @Test
    @DisplayName("update throws for a nonexistent producto")
    void update_missingProducto_throws() {
        UUID id = UUID.randomUUID();
        when(productoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.update(id, request("New")))
                .isInstanceOf(ProductoNotFoundException.class);
    }

    @Test
    @DisplayName("delete removes an existing producto")
    void delete_existingProducto_removesIt() {
        UUID id = UUID.randomUUID();
        Producto existing = Producto.builder().id(id).name("X").build();
        when(productoRepository.findById(id)).thenReturn(Optional.of(existing));

        productoService.delete(id);

        verify(productoRepository).delete(existing);
    }

    @Test
    @DisplayName("delete throws for a nonexistent producto instead of silently no-op'ing")
    void delete_missingProducto_throws() {
        UUID id = UUID.randomUUID();
        when(productoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.delete(id))
                .isInstanceOf(ProductoNotFoundException.class);
        verify(productoRepository, never()).delete(any());
    }
}
