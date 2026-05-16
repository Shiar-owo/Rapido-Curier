package com.rapidocurier.authservice.application.port.in;

import com.rapidocurier.authservice.domain.model.Usuario;

public interface RegisterUseCase {
    Usuario registrar(String nombre, String email, String password, String rol);
}