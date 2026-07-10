package com.konverza.empresa.service;

import com.konverza.empresa.dto.EmpresaRequest;
import com.konverza.empresa.entity.Empresa;
import com.konverza.empresa.exception.EmpresaNotFoundException;
import com.konverza.empresa.repository.EmpresaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Empresa is a singleton — at most one row ever exists (add-users-empresa-profile).
 * {@link #get()} 404s until the first {@link #upsert} call creates it; every
 * upsert afterward updates that same row in place instead of inserting a new one.
 */
@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public Empresa get() {
        return firstExisting().orElseThrow(EmpresaNotFoundException::new);
    }

    @Transactional
    public Empresa upsert(EmpresaRequest req) {
        Empresa empresa = firstExisting().orElseGet(Empresa::new);
        empresa.setName(req.getName());
        empresa.setContext(req.getContext());
        return empresaRepository.save(empresa);
    }

    private Optional<Empresa> firstExisting() {
        List<Empresa> all = empresaRepository.findAll();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }
}
