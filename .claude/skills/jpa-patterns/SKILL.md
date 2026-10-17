---
name: jpa-patterns
description: Padrões JPA/Hibernate deste projeto — entidade rica, relações sem N+1, transações curtas, paginação e índices via Flyway. Use ao criar/alterar entidades, repositórios, queries JPQL ou ao investigar N+1.
metadata:
  origin: Adaptado de ECC (affaan-m/ECC, MIT) para os fundamentos do CLAUDE.md
---

# Padrões JPA/Hibernate

O `CLAUDE.md` prevalece sobre esta skill em caso de dúvida.

## Entidade

Siga o formato das entidades existentes (ex.: `compras/contas/ContaAPagar.java`):

- Construtor `protected` vazio só para o JPA; construtor real `private` que valida tudo e lança
  `DomainException`. Criação por método de fábrica com nome de intenção (`lancar`, `parcelaDaNota`).
- Sem setters. Mudança de estado por método de negócio (`pagar(quem)`, `cancelar()`), que verifica
  a transição antes de mudar.
- Acessores no estilo record (`valor()`, `status()`); campo opcional volta como `Optional`.
- `@Column(updatable = false)` em tudo que não muda depois de criado.
- `@Enumerated(EnumType.STRING)`, nunca ordinal.
- Tamanhos e limites como constantes nomeadas, usadas no `@Column` e na validação.
- Id `UUID` (`GenerationType.UUID`), como no restante do schema.

## Relações e N+1

- Padrão do projeto: agregados se referenciam por `UUID` (ex.: `ContaAPagar.contatoId`), não por
  `@ManyToOne`. Partes que pertencem à raiz (itens do `Pedido`, da `NotaEntrada`) são
  `@ElementCollection` de objeto de valor. Só crie relação JPA entre entidades com justificativa.
- Se houver `@ManyToOne`, use `fetch = LAZY`; o padrão EAGER do JPA para `@ManyToOne` causa N+1 em listas.
- Nunca `EAGER` em coleção.
- Coleção interna não sai crua: o getter devolve `Collections.unmodifiableList(...)` ou `List.copyOf(...)`.
  Itens entram e saem por métodos da raiz (`adicionarItem`, `removerItem`).
- Quando a tela precisa da coleção, busque junto: `left join fetch` na query do repositório.
- Em listagens, prefira projeção (interface ou record) só com as colunas usadas.
- Ao mexer em relação, ative `logging.level.org.hibernate.SQL=DEBUG` localmente e confira quantas
  queries a operação gera.

```java
@Query("select p from Pedido p left join fetch p.itens where p.id = :id")
Optional<Pedido> buscarComItens(@Param("id") UUID id);
```

## Repositório

- Spring Data derivado ou JPQL com `:parametro`. Concatenar string em query é proibido.
- Paginação com `Pageable`/`PageRequest`; ordenação explícita.

## Transações

- `@Transactional` no service, no método público que faz a operação; nunca em controller.
- `@Transactional(readOnly = true)` em leitura.
- Escopo mínimo: nada de chamada HTTP externa (Mercado Pago, W-API, Claude) dentro da transação.
- A entidade não sai do service: converta para record DTO antes de devolver.

## Schema e índices

- Toda mudança de schema é migration Flyway nova em `src/main/resources/db/migration`. Nunca edite
  migration já aplicada.
- Índice para filtros frequentes e para toda FK; composto na ordem igualdade → intervalo.

## Testes de persistência

- Repositório/query com Testcontainers (PostgreSQL real, nunca H2).
