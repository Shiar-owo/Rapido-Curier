package com.rapidocurier.authservice.infrastructure.config;

import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.entity.RolEntity;
import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.repository.RolJpaRepository;
import com.rapidocurier.authservice.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioJpaRepository usuarioRepository;
    private final RolJpaRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) return;

        RolEntity adminRol = rolRepository.findByNombre(RolEntity.RolNombreEntity.ADMIN)
                .orElseGet(() -> rolRepository.save(new RolEntity(null, RolEntity.RolNombreEntity.ADMIN)));
        RolEntity operadorRol = rolRepository.findByNombre(RolEntity.RolNombreEntity.OPERADOR)
                .orElseGet(() -> rolRepository.save(new RolEntity(null, RolEntity.RolNombreEntity.OPERADOR)));
        RolEntity clienteRol = rolRepository.findByNombre(RolEntity.RolNombreEntity.CLIENTE)
                .orElseGet(() -> rolRepository.save(new RolEntity(null, RolEntity.RolNombreEntity.CLIENTE)));

        UsuarioEntity admin = new UsuarioEntity();
        admin.setNombre("Admin Rapido");
        admin.setEmail("admin@rapidocourier.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles(Set.of(adminRol));

        UsuarioEntity operador = new UsuarioEntity();
        operador.setNombre("Operador Rapido");
        operador.setEmail("operador@rapidocourier.com");
        operador.setPassword(passwordEncoder.encode("operador123"));
        operador.setRoles(Set.of(operadorRol));

        UsuarioEntity cliente = new UsuarioEntity();
        cliente.setNombre("Cliente Rapido");
        cliente.setEmail("cliente@rapidocourier.com");
        cliente.setPassword(passwordEncoder.encode("cliente123"));
        cliente.setRoles(Set.of(clienteRol));

        usuarioRepository.saveAll(Set.of(admin, operador, cliente));
    }
}