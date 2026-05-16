package com.rapidocurier.authservice.domain.port.out;

import com.rapidocurier.authservice.domain.model.Usuario;

public interface JwtPort {
    String generarToken(Usuario usuario);
}