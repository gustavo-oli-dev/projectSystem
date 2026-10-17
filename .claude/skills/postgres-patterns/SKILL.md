---
name: postgres-patterns
description: Padrões PostgreSQL para migrations Flyway deste projeto — índices, tipos de coluna, upsert, paginação e consultas de diagnóstico. Use ao escrever migration, criar índice ou investigar query lenta.
metadata:
  origin: Adaptado de ECC (affaan-m/ECC, MIT; base Supabase Agent Skills, MIT) para este projeto
---

# Padrões PostgreSQL

O `CLAUDE.md` prevalece. Todo SQL de schema vai em migration Flyway nova
(`backend/src/main/resources/db/migration/V<n>__descricao.sql`); migration aplicada não é editada.

## Tipos

| Uso | Tipo |
|---|---|
| Id | `uuid` (padrão do projeto) |
| Data/hora de evento | `timestamptz` |
| Data sem hora (vencimento) | `date` |
| Dinheiro | `numeric(14, 2)` (padrão das migrations), nunca `float`/`real` |
| Flag | `boolean` |
| Enum do Java | `varchar` com o nome da constante (`EnumType.STRING`) |
| Texto livre | `text`, ou `varchar(n)` quando a entidade tem limite nomeado |

## Índices

| Padrão da query | Índice |
|---|---|
| `where col = x` / `where col > x` | B-tree: `create index ... on t (col)` |
| `where a = x and b > y` | Composto, igualdade antes de intervalo: `(a, b)` |
| Só linhas ativas | Parcial: `... (col) where status = 'ABERTA'` |
| Busca por prefixo/texto | `lower(col)` com B-tree, ou `pg_trgm` se justificado |

- Toda FK ganha índice (o Postgres não cria sozinho).
- Unicidade de regra de negócio (e-mail, código de barras) vira `unique` no banco também.

## Escrita e leitura

- Upsert: `insert ... on conflict (chave) do update set col = excluded.col`.
- Fila de processamento concorrente: `for update skip locked`.
- Listas grandes: paginação por cursor (`where criada_em < :ultimo order by criada_em desc limit n`)
  é mais barata que `offset` alto.

## Diagnóstico (local, via psql)

```sql
-- FKs sem índice
select conrelid::regclass, a.attname
from pg_constraint c
join pg_attribute a on a.attrelid = c.conrelid and a.attnum = any(c.conkey)
where c.contype = 'f'
  and not exists (
    select 1 from pg_index i
    where i.indrelid = c.conrelid and a.attnum = any(i.indkey)
  );

-- plano de uma query suspeita
explain (analyze, buffers) select ...;
```

## Fora do escopo

Configurar o servidor (`alter system`, `max_connections`, extensões) não se aplica: em produção o
Postgres é gerenciado pelo Render. RLS também não se aplica, porque a autorização fica no backend.
