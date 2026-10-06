package com.empresax.sistema.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler tratador = new GlobalExceptionHandler();

    @Test
    void parametroFaltandoEhErroDeQuemChamouENaoDoServidor() {
        var resposta = tratador.tratarParametroAusente(new MissingServletRequestParameterException("data", "LocalDate"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void caminhoInexistenteResponde404() {
        var resposta = tratador.tratarCaminhoInexistente(new NoResourceFoundException(HttpMethod.GET, "api/nao-existe"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void metodoErradoResponde405() {
        var resposta = tratador.tratarMetodoNaoSuportado(new HttpRequestMethodNotSupportedException("DELETE"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }
}
