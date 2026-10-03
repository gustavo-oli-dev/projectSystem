# Modelo de Domínio

Visão geral das entidades do sistema, organizadas pela fatia de entrega em que aparecem (roadmap
completo em [DECISOES.md](../DECISOES.md)). Nomes em português, no idioma do domínio de negócio.

## Fatia 1 — Fundação (implementada)

### Usuario
- Atributos: id, nome, email (único), senhaCriptografada, acessoIrrestrito, cargo (opcional), ativo
- Ou tem acesso irrestrito (dono, programador), ou tem um cargo — nunca nenhum dos dois (fábricas
  `comCargo`/`comAcessoIrrestrito` + CHECK no banco). `permissoes()`, `possui(p)`,
  `podeConceder(perms)` (anti-escalada) e `trocarCargo(c)` ficam na entidade (D14).

### Cargo / Permissao
- `Permissao`: catálogo fixo (enum) com "ver" e "gerenciar" por área — cada uma é uma checagem
  `@PreAuthorize` real no servidor.
- `Cargo`: nome (único, ≤ 80), descrição (≤ 255), conjunto de permissões (ao menos uma). Validado
  por inteiro antes de qualquer alteração em `redefinir(...)`. Não pode ser excluído em uso.
- Regra: e-mail e nome validados no construtor; usuário nasce ativo; `desativar()`/`ativar()` são
  as únicas formas de mudar esse estado (Tell, Don't Ask — nunca um `setAtivo(boolean)`)

### Cliente
- Atributos: id, nome, documento (`Documento`), telefoneWhatsapp, criadoEm
- O documento (CPF/CNPJ) é imutável após o cadastro — trocar o documento de um cliente existente é
  tratado como um caso à parte, fora do escopo da v1, não como um `update` comum

### Value Objects compartilhados
- `Documento` (sealed interface): `Cpf` ou `Cnpj`, cada um validando formato e dígito verificador
  no construtor compacto do record — o objeto nunca existe em estado inválido
- `Cnpj` aceita tanto o formato numérico clássico (14 dígitos) quanto o alfanumérico instituído
  pela IN RFB 2.229/2024, válido nacionalmente desde julho de 2026: 12 caracteres alfanuméricos +
  2 dígitos verificadores numéricos. **O algoritmo do dígito verificador alfanumérico foi
  implementado conforme a especificação pública da Receita Federal, mas ainda não foi conferido
  contra o validador oficial — fazer essa conferência antes de ir para produção.**
- `Documento.criar(String)`: fábrica que decide `Cpf` vs `Cnpj` pelo tamanho do valor normalizado —
  ponto único de decisão, nenhum `if`/`switch` repetido pelo resto do sistema

## Fatia 2 — Núcleo fiscal (implementada parcialmente — falta a emissão de verdade)

### Produto / Servico
- `Produto` → NF-e: nome, descrição, NCM (8 dígitos), unidade de medida, preço unitário (`Dinheiro`)
- `Servico` → NFS-e: nome, descrição, código LC 116/2003 (formato `NN.NN`), alíquota de ISS
  (validada entre 2% e 5%, limites nacionais da LC 116/2003 + LC 157/2016), preço unitário
- CST/CSOSN e demais atributos que dependem do regime tributário da empresa ainda não entraram —
  aguardando a pendência A1 em DECISOES.md

### Pedido (agregado raiz)
- Contém N `ItemPedido` (`@ElementCollection`, cada item é um snapshot: tipo, referência, descrição
  e preço **congelados no momento da compra** — mudar o preço do Produto/Servico depois não afeta
  pedidos já criados)
- Estados: `ABERTO` → `AGUARDANDO_EMISSAO` → `CONCLUIDO` | `CANCELADO` (guard clauses na própria
  entidade — nenhum `if` de transição de estado vive no service)
- `PedidoService.criar` busca Produto/Servico pelo id, copia preço e descrição para o `ItemPedido`
- Um pedido com item de produto e item de serviço vai gerar **dois** `DocumentoFiscal`
  independentes quando a emissão de verdade existir; o pedido só conclui quando todos estiverem
  autorizados (falha parcial precisa de compensação, não de um status único "nota emitida")

### DocumentoFiscal
- Tipo: `NFE` | `NFSE`. Estados: `PENDENTE` → `AUTORIZADO` | `REJEITADO` → `CANCELADO`
- Guarda o XML autorizado e o protocolo da SEFAZ/prefeitura — isso É o documento fiscal; o
  DANFE/DANFSE em PDF é derivado dele, nunca armazenado como fonte da verdade
- **Modelo e persistência prontos, mas sem repository/service/controller ligados ao Pedido ainda**:
  a chamada real à SEFAZ (NF-e) ou ao provedor fiscal (NFS-e, Focus NFe) depende do regime
  tributário da empresa e de credenciais que ainda não existem — ver PENDENCIAS.md

## Fatia 3 — Financeiro (implementada)

### Cobranca
- Vinculada a um Pedido `AGUARDANDO_EMISSAO`. Meio: `PIX` | `BOLETO`.
  Estados: `PENDENTE` → `PAGA` | `VENCIDA` | `CANCELADA`
- Criada via `ProvedorPagamento` (porta), implementada por `MercadoPagoProvedorPagamento` (adapter
  HTTP real, sem dependência nova — usa `java.net.http.HttpClient` + Jackson, já transitivos)
- Baixa automática via webhook: `MercadoPagoWebhookController` (endpoint público, validado por
  HMAC) só grava um evento outbox; `WebhookPagamentoProcessor` processa assíncrono e sempre
  confirma o status consultando a API antes de dar baixa — nunca confia só no payload recebido
- Partes com confiança média, nunca testadas contra a API real (sem credenciais disponíveis):
  o corpo da criação de pagamento, os campos lidos na resposta, e o formato da assinatura do
  webhook — ver PENDENCIAS.md

## Fatia 4 — Atendimento (implementada)

### Conversa
- Identificada pelo **telefone**, não pelo Cliente: a primeira mensagem chega antes de qualquer
  cadastro existir (RF01). `clienteId` fica nulo até existir (ou ser formalizado) um Cliente com
  aquele telefone — vínculo automático só na criação, retroativo é um gap conhecido (não
  implementado nesta fatia)
- Estados: `ABERTA` → `ENCERRADA`. `atendenteId` atribuído via `atribuirAtendente`

### Mensagem / Anexo
- `Mensagem` pertence a uma `Conversa` (campo `conversaId`, não uma referência JPA — mesmo padrão
  de `Cobranca.pedidoId`/`DocumentoFiscal.pedidoId`)
- `Anexo`: metadado (MIME, tamanho, checksum SHA-256, chave do objeto) no Postgres; os bytes ficam
  no MinIO via a porta `ArmazenamentoObjetos` (decisão D8 em DECISOES.md)

### WhatsAppGateway (porta) / EvolutionApiWhatsAppGateway (adapter)
- Isola a API não oficial (decisão D2) do resto do sistema — trocar de provedor não toca em
  `MensagemService`
- Entrada de mensagens pelo mesmo padrão outbox do financeiro: webhook grava o payload bruto,
  `WebhookWhatsAppProcessor` interpreta de forma assíncrona

## Fatia 5 — IA (pendente)

- IA do cliente: as ferramentas recebem a identidade já resolvida pelo backend a partir do número
  de WhatsApp — nunca aceitam identidade vinda do próprio modelo
- IA do gestor: somente leitura e sugestões (decisão do usuário, 02/10/2026) — nenhuma ferramenta
  de escrita é exposta a ela
