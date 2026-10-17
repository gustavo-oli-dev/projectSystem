---
name: springboot-tdd
description: Como escrever testes neste projeto — unitários sem Spring para regra de domínio e services, integração com Testcontainers (PostgreSQL real), nomes que descrevem cenário e resultado. Use ao adicionar regra de negócio, corrigir bug ou refatorar backend.
metadata:
  origin: Adaptado de ECC (affaan-m/ECC, MIT) para os fundamentos do CLAUDE.md
---

# Testes no backend

O `CLAUDE.md` prevalece sobre esta skill.

## Fluxo

1. Escreva o teste do comportamento esperado (ou do bug) e veja falhar.
2. Implemente o mínimo para passar.
3. Refatore com o teste verde.

Toda regra de negócio nova vem com teste. Não existe meta de cobertura numérica nem JaCoCo no
projeto; não adicione sem justificar.

## Unitário de domínio (o principal)

A regra mora na entidade, então o teste mais valioso cria a entidade e exercita o método de negócio
(ex.: `pedido/PedidoTest.java`, `cobranca/CobrancaTest.java`). Sem Spring, sem mock.

```java
@Test
void pagarContaJaCanceladaEhRecusado() {
    ContaAPagar conta = ContaAPagar.lancar(null, "Luz", VALOR, VENCIMENTO, RESPONSAVEL);
    conta.cancelar();

    assertThatThrownBy(() -> conta.pagar(RESPONSAVEL))
            .isInstanceOf(DomainException.class);
}
```

## Unitário de service

Sem subir contexto do Spring: mocks com `mock(...)` e o service montado pelo construtor, como em
`venda/CancelamentoVendaServiceTest.java`. Nada de `@ExtendWith(SpringExtension)`, `@MockBean` ou
`@Autowired`.

- Afirme o resultado observável: estado da entidade, valor devolvido, exceção.
- `verify(...)` só para efeito colateral que é o próprio comportamento (estoque devolvido, cobrança
  estornada). Não use `verify(repo).save(any())` só para provar que o código chamou o repositório.
- Valores de teste em constantes nomeadas (`PRECO`, `RESPONSAVEL`), não números soltos.

## Integração

- Banco: Testcontainers com PostgreSQL real (dependências já no `pom.xml`). H2 é proibido.
- Prefira `@ServiceConnection` no container para o Spring pegar a URL sozinho.
- Use integração para o que o unitário não cobre: migration Flyway, query JPQL, N+1, segurança dos
  endpoints (`spring-security-test`), formato do erro do `GlobalExceptionHandler`.

## Nomes

O nome descreve cenário e resultado, em português e em camelCase:
`cancelarPedidoConfirmadoDevolveOsProdutosAoEstoque`, `descontoAcimaDoLimiteExigeAutorizacao`.

## Rodar

- `mvn test` dentro de `backend/` (o build do Docker usa `-DskipTests`, então rode à parte).
- Integração com Testcontainers precisa do Docker (Colima) ligado.
