package com.empresax.sistema.acesso.web;

import com.empresax.sistema.acesso.Cargo;
import com.empresax.sistema.acesso.CargoService;
import com.empresax.sistema.acesso.Permissao;
import com.empresax.sistema.acesso.RegraAcesso;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
public class CargoController {

    private final CargoService cargoService;

    public CargoController(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    /** Listar cargos também serve a quem gerencia usuários (para escolher o cargo de alguém). */
    @GetMapping("/api/cargos")
    @PreAuthorize(RegraAcesso.CARGOS_GERENCIAR + " or " + RegraAcesso.USUARIOS_GERENCIAR)
    public List<CargoResponse> listar() {
        return cargoService.listar().stream().map(CargoResponse::de).toList();
    }

    @GetMapping("/api/permissoes")
    @PreAuthorize(RegraAcesso.CARGOS_GERENCIAR)
    public List<PermissaoResponse> catalogo() {
        return Arrays.stream(Permissao.values()).map(PermissaoResponse::de).toList();
    }

    @PostMapping("/api/cargos")
    @PreAuthorize(RegraAcesso.CARGOS_GERENCIAR)
    public ResponseEntity<CargoResponse> criar(
            @Valid @RequestBody CargoRequest requisicao,
            @AuthenticationPrincipal UserDetails ator
    ) {
        Cargo cargo = cargoService.criar(ator.getUsername(), requisicao.nome(), requisicao.descricao(), requisicao.permissoes());
        return ResponseEntity.created(URI.create("/api/cargos/" + cargo.id())).body(CargoResponse.de(cargo));
    }

    @PutMapping("/api/cargos/{id}")
    @PreAuthorize(RegraAcesso.CARGOS_GERENCIAR)
    public CargoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody CargoRequest requisicao,
            @AuthenticationPrincipal UserDetails ator
    ) {
        return CargoResponse.de(cargoService.atualizar(
                ator.getUsername(), id, requisicao.nome(), requisicao.descricao(), requisicao.permissoes()));
    }

    @DeleteMapping("/api/cargos/{id}")
    @PreAuthorize(RegraAcesso.CARGOS_GERENCIAR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        cargoService.excluir(id);
    }
}
