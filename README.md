# Sistema de Vendas — Empresa X

Painel administrativo + API para vendas com NF-e (produto) e NFS-e (serviço), atendimento via
WhatsApp e emissão fiscal integrada à SEFAZ-CE / município de Fortaleza.

As regras de engenharia do projeto estão em [CLAUDE.md](CLAUDE.md) — leia antes de alterar código.
As decisões de arquitetura e negócio estão em [DECISOES.md](DECISOES.md).
O modelo de domínio completo está em [docs/MODELO-DOMINIO.md](docs/MODELO-DOMINIO.md).
O que falta validar/testar antes de produção está consolidado em [PENDENCIAS.md](PENDENCIAS.md).

## Status

Construídas: **Fundação** (autenticação, perfis, clientes), **Núcleo fiscal, parcial** (produtos,
serviços, pedidos, modelo de `DocumentoFiscal` sem emissão real ainda), **Financeiro** (cobrança
via Mercado Pago) e **Atendimento** (conversas de WhatsApp, inbox, anexos no MinIO). Falta só a
fatia de **IA** — ver o roadmap em DECISOES.md.

**Ainda não foi possível rodar nada**: Docker Desktop e WSL2 já instalados, mas a **virtualização
está desligada na BIOS/UEFI** desta máquina — é preciso entrar na BIOS e habilitar Intel VT-x/AMD
SVM antes de o Docker funcionar (ver DECISOES.md).

Este código ainda não foi compilado nem executado: a máquina onde foi escrito não tem Java, Maven,
Node ou Docker instalados. Revisar com atenção antes do primeiro `docker compose up`.

## Stack

- Backend: Java 21 + Spring Boot 3, Maven
- Banco: PostgreSQL (Flyway para migrations)
- Anexos: MinIO (S3-compatível) — ver decisão D8
- Frontend: TypeScript puro (sem framework), compilado com `tsc`, servido como módulos ES nativos
- Execução: Docker Compose

## Como rodar

1. Copie `.env.example` para `.env` e preencha os valores (nunca commitar `.env`).
2. `docker compose up --build`
3. Backend: http://localhost:8080 — documentação OpenAPI em `/swagger-ui.html`
4. Frontend: http://localhost:8081

## O que já existe

- Cadastro e autenticação de usuários (perfis admin/financeiro/atendente), login com JWT stateless
- Cadastro de clientes com validação de CPF/CNPJ (inclusive CNPJ alfanumérico, IN RFB 2.229/2024)
- Cadastro de produtos (NCM) e serviços (código LC 116/2003, alíquota de ISS)
- Pedidos: criação com itens de produto/serviço (preço congelado na hora da compra), confirmação e
  cancelamento, com total calculado em `Dinheiro` (nunca `float`/`double`)
- Modelo de `DocumentoFiscal` (NF-e/NFS-e) pronto, com seu ciclo de vida — ainda não ligado a uma
  emissão real
- Cobrança via Mercado Pago (Pix/boleto) com baixa automática por webhook (outbox assíncrono,
  nunca confia direto no payload — sempre confirma com uma consulta à API)
- Atendimento via WhatsApp (Evolution API): conversas por telefone, vínculo automático a cliente
  cadastrado, inbox (fila de conversas abertas, atribuição de atendente), anexos com metadado no
  Postgres e bytes no MinIO
- Tratamento de erro centralizado (`@ControllerAdvice`), sem vazamento de detalhe interno
- Testes unitários das regras de negócio de todas as entidades acima (ver PENDENCIAS.md para o que
  ainda falta em testes de integração)

## O que falta (ver PENDENCIAS.md para a lista completa a validar/testar de uma vez)

- Compilar e rodar pela primeira vez — nunca foi executado nesta máquina
- Testes de integração com Testcontainers
- Emissão fiscal de verdade (NF-e via lib própria, NFS-e via Focus NFe) — depende do regime
  tributário da empresa e de credenciais do provedor

## Próximas fatias (ver DECISOES.md)

- Emissão fiscal de verdade ligada ao Pedido/DocumentoFiscal
- As duas instâncias de IA (cliente e gestor), cada uma com acesso restrito pelo backend
- Tags e SLA no inbox de atendimento (RF07)
