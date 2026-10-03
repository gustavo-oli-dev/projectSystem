package com.empresax.sistema.common.web;

import java.time.Instant;

public record ApiErrorResponse(Instant timestamp, int status, String mensagem) {
}
