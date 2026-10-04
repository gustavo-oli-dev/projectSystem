# Decisões de Arquitetura e Negócio

Registro vivo das decisões tomadas fora do código (jurídicas, de produto e de arquitetura).
Atualizar sempre que uma decisão for tomada ou revista — nunca apagar o histórico, só adicionar.

## Locadas (2026-10-02)

| # | Decisão | Justificativa / observação |
|---|---|---|
| D1 | Localização: Fortaleza/CE | NF-e → SEFAZ-CE. NFS-e → sistema municipal de Fortaleza, em migração para o padrão nacional (LC 214/2025). Confirmar com a SEFIN Fortaleza ou com o provedor fiscal o estágio atual da migração. |
| D2 | WhatsApp: API **não oficial** | Risco aceito pelo usuário (ban do número, ausência de DPA formal). Mitigação: camada de abstração `WhatsAppGateway` para troca futura sem reescrever regra de negócio; recomendado **Evolution API** (self-hosted, Docker, wraps Baileys) por se encaixar em "tudo roda em Docker" sem infra nova. |
| D3 | Emissão fiscal: NF-e por biblioteca própria, NFS-e por provedor fiscal | Mantido da análise anterior. Provedor recomendado para NFS-e (e DANFE/consulta): **Focus NFe** — cobertura nacional incluindo Fortaleza, sandbox de homologação. |
| D4 | PSP de pagamento: **Mercado Pago** | Pix + boleto. Webhook validado por `x-signature` (HMAC) + confirmação via GET na API (nunca confiar só no payload do webhook). Ver seção de reconciliação abaixo. |
| D5 | Disponibilidade (RNF01): risco de host único aceito | Compose em um único host. Sem multi-host na v1. Revisitar se SLA de 99,5% não for atingido na prática. |
| D6 | IA do gestor: **somente leitura e sugestões** | Nenhuma ação (emissão, cancelamento, cobrança) é disparada pela IA sem confirmação humana explícita no fluxo de negócio. Reduz drasticamente a superfície de risco de prompt injection indireta. |
| D7 | Certificado digital: **A1** | A3 (token físico) não opera em container sem HSM/nuvem adicional. A1 é injetado como segredo em runtime, nunca em imagem ou repositório, com alerta de vencimento. |
| D8 | Anexos de WhatsApp: metadados no Postgres, bytes no **MinIO** | Fonte da verdade (remetente, timestamp, MIME, tamanho, checksum, chave do objeto) fica no Postgres. Os bytes da mídia ficam no MinIO (self-hosted, Docker), com política de backup própria — não "sem backup", um backup dimensionado para esse tipo de dado. Ver raciocínio completo abaixo. |
| D9 | Autenticação: **JWT stateless** (Spring Security) | Default de engenharia, não decisão de negócio — API interna + painel web, sem necessidade de SSO/OAuth na v1. Token no header `Authorization`, sem cookie/sessão (por isso CSRF pode ficar desabilitado, justificado em código). Revisar se um dia entrar SSO corporativo. |
| D10 | RF04 confirmado: `Cpf`/`Cnpj` como Value Objects validando no construtor, **incluindo CNPJ alfanumérico** (IN RFB 2.229/2024, válido desde jul/2026) | Implementado em `Documento`/`Cpf`/`Cnpj`. O dígito verificador alfanumérico segue a especificação pública da Receita Federal mas ainda não foi conferido contra o validador oficial — pendência antes de produção, documentada no próprio código. |

| D12 | WhatsApp via **W-API** (hospedada), substituindo a Evolution API | Revisa D2 (02/10): o usuário já usa a W-API (painel.w-api.app). Continua não oficial (risco de banimento aceito). Adapter `WApiWhatsAppGateway` conforme a OpenAPI oficial (POST /v1/message/send-text, Bearer token + instanceId). Webhook protegido por token secreto na URL (a W-API não permite cabeçalho customizado). Evolution removida do código e do compose. |
| D13 | Conversas **dirigidas por bot** (Claude), com assunção humana | O bot responde o cliente automaticamente; o atendente pode assumir (o bot para) ou devolver ao bot. O bot transfere sozinho quando o cliente pede uma pessoa ou o assunto foge do escopo. Chave da Claude separada da do gestor. Ferramentas só de consulta, com a identidade do cliente resolvida pelo servidor (nunca pelo modelo) e sem CPF/CNPJ nas respostas. Se a IA ou o WhatsApp falharem, a conversa vai para a fila humana. |
| D18 | **Estoque, fotos e reembolso** — preparação para o site de vendas (03/10) | Estoque fica no produto e nunca fica negativo (regra na entidade, CHECK no banco e trava `SELECT ... FOR UPDATE` contra duas vendas da mesma última unidade). Pedir mais do que existe já é recusado na criação do pedido; a **baixa acontece na confirmação**; cancelamento ou reembolso devolve ao estoque. Toda mudança fica em `movimentacoes_estoque` (quem, quando, qual pedido). **Reembolso** é ação separada (pedido pago): devolve o dinheiro pelo Mercado Pago com chave de idempotência, cancela o pedido e devolve o estoque; "Cancelar" só vale para pedido não pago e cancela também a cobrança pendente no Mercado Pago. **Fotos no PostgreSQL** (bytea, até 5 MB, 8 por produto, tipo conferido pelos bytes): o backup diário cobre só o banco e o MinIO não está rodando. Foto e vitrine (`/api/loja/produtos`) são públicas e só de leitura. Código de barras EAN/GTIN com dígito verificador, pronto para o app de escaneamento e para o cEAN da NF-e. Nova permissão "Dar entrada no estoque". |
| D19 | **Caixa (PDV) — venda presencial** com estoque único (03/10) | Mesmo estoque do site e do WhatsApp. Leitor de código de barras USB/Bluetooth (funciona como teclado; os produtos usam o EAN de fábrica). Venda de balcão não exige cliente cadastrado (CPF na nota opcional) e gera **NFC-e** (modelo 65), não NF-e — o canal da venda decide o documento. A venda acontece numa transação só: pedido → baixa no estoque → pagamento → NFC-e pendente. Maquininha: fornecedor ainda não definido, então **modo maquininha avulsa** — o operador digita bandeira e código de autorização (gravado como `maquininhaIntegrada = false`, o que a NFC-e informa). Não existe aprovação simulada. A regra estadual que o usuário citou exige maquininha **integrada** ao emissor da NFC-e: quando o fornecedor for escolhido, um adapter manda o valor para a maquininha e recebe a confirmação, substituindo a digitação. Cancelar venda do caixa exige permissão própria (estorno é ponto sensível a fraude) e só acontece pelo Caixa, que estorna o pagamento. Entrada de estoque por leitura: escolhe o produto e cada leitura conta +1; produto sem código vincula o lido. |
| D20 | **Maquininha integrada** ao caixa (03/10) | Pedido do usuário: "selecionar o produto e mandar para a maquininha", sem digitar autorização — é também o que a regra estadual de maquininha integrada à NFC-e exige. Porta `Maquininha` com adapter **Mercado Pago Point** (mesma conta do Pix; fornecedor ainda não confirmado — trocar é trocar só o adapter). Fluxo: venda separa o estoque → valor vai para a maquininha → o caixa consulta a cada 2 s → aprovado grava bandeira/autorização vindas da maquininha (`maquininhaIntegrada = true`) e gera a NFC-e; recusado permite tentar de novo; desistência tira o valor da maquininha e devolve o estoque; venda aprovada cancelada é estornada pelo fornecedor. A digitação manual ficou só como **contingência** (maquininha sem conexão), registrada como não integrada. |
| D21 | **Painel = central de relatórios** (03/10) | Pedido do usuário ("todo tipo de estatística"). "Venda" = pedido confirmado e não cancelado, contado na **data da confirmação** (`pedidos.confirmado_em`) no **fuso de Fortaleza**. Indicadores com comparação ao período anterior de mesmo tamanho; faturamento por dia/semana/mês (agrupamento automático pelo tamanho do período); canal; formas de pagamento (+ a receber); horário de pico; dia da semana; mais vendidos; exportação CSV para Excel (";", vírgula decimal, BOM, proteção contra injeção de fórmula). **Lucro** exige custo: novo campo `custo_unitario` no produto e **custo congelado no item vendido** (mudar o custo não reescreve o passado); sem custo, o lucro é mostrado como parcial ou "custo não informado", nunca inventado. Custo só aparece para quem gerencia o catálogo ou vê o faturamento. Gráficos em SVG próprio seguindo o método de dataviz: uma série = uma cor (#2a78d6, validada contra a superfície), colunas ≤24px, grade fina, dica no mouse e no teclado, tabela equivalente em cada gráfico. Valores em dinheiro exigem FATURAMENTO_VER; sem ela, o painel mostra só a parte operacional. |
| D21a | **Painel em grade de 3 colunas com pizzas (rosca)** (03/10) | Pedido do usuário ("bagunçado, gráficos em pizza"). Pizza só onde é **parte de um todo** com até 6 categorias: formas de pagamento (+ a receber), canal, custo × lucro (+ vendido sem custo) e pedidos por situação. Faturamento no tempo, horário e dia da semana **continuam em colunas**: em pizza, 30 dias ou 24 horas viram fatias impossíveis de comparar. Cores das fatias = paleta categórica validada (`validate_palette.js`, inclusive o par última↔primeira do anel), fixas por categoria; o "restante" (a receber, sem custo) é cinza claro. Legenda sempre com valor e %, 2px de folga entre fatias, destaque no mouse/teclado. Indicadores reduzidos a 4 do mesmo tamanho (unidades e desfeitas viraram nota). Barras horizontais removidas. |
| D11 | Interface do painel segue a estrutura de CRMs como Pipedrive (referência do usuário em 02/10) | Topbar com busca + ação rápida, menu lateral agrupado (Vendas / Catálogo / Operação), tabelas largas com contagem de registros, status como ponto colorido + texto. Áreas: Painel, Pedidos, Clientes, Produtos, Serviços, Fiscal · SEFAZ, Cobranças, Atendimento. |
| D14 | Hierarquia de acesso: **cargos configuráveis** sobre um catálogo fixo de permissões; dono e programador com **acesso irrestrito** | Pedido do usuário em 02/10. Substitui o perfil fixo ADMIN/FINANCEIRO/ATENDENTE (RF09). Cada área tem "ver" e "gerenciar" separados (ex.: gerente vê pedidos mas não faturamento nem SEFAZ). O catálogo é fixo no código porque cada permissão é uma checagem real no servidor (`@PreAuthorize`); o que se configura na tela **Funcionários e Cargos** são os cargos. Anti-escalada: só se concede o que se tem; ninguém troca o próprio cargo; só irrestrito cria ou altera irrestrito. Funcionário desativado perde o acesso na hora (permissões relidas do banco a cada requisição). O primeiro acesso irrestrito vem do e-mail em `ACESSO_INICIAL_EMAIL`. O frontend só esconde o que o cargo não usa; quem protege é o backend. |
| D14a | Na tela, "cargo" passa a se chamar **"Perfil de acesso"** (03/10) | Pedido do usuário: "cargo" já é a função real da pessoa na empresa e confundia. Mudou só o texto visível (área "Funcionários e Perfis de acesso"). No código e no banco o nome continua `Cargo` / `cargos` / `CARGOS_GERENCIAR` — leia como "perfil de acesso". Renomear o código não muda nada para quem usa e só traria risco. |
| D15 | Assistente do gestor como **chat dentro do painel**, liberado por cargo | Botão "Assistente" na topbar para quem tem `ASSISTENTE_GESTOR_USAR`. A IA só recebe as ferramentas de consulta que o cargo de quem pergunta permite (faturamento exige `FATURAMENTO_VER` etc.), e a execução confere de novo. Toda pergunta fica registrada com quem perguntou. O WhatsApp do dono continua com acesso total. |
| D16 | Assistente também **pelo WhatsApp de cada funcionário**, com número verificado | Pedido do usuário em 03/10 ("botão que adicione um bot no WhatsApp"). Botão "Usar no WhatsApp" no chat do assistente: a pessoa informa o próprio número, recebe um código de 6 dígitos pela W-API e confirma no painel. Sem essa verificação, alguém poderia cadastrar o número de outra pessoa e o assistente enviaria dados da empresa a ela. Código guardado só como hash, vale 10 min, 5 tentativas, 1 envio por minuto. Mensagens desse número vão ao assistente com as permissões do cargo da pessoa, relidas a cada mensagem (desativado ou sem a permissão → resposta de acesso encerrado). |
| D17 | **Dois números de WhatsApp**: empresa (clientes) e interno (assistente) | Pedido do usuário em 03/10 para evitar vazamento de dados; revisa D16. Cada número é uma instância própria da W-API, com webhook e token próprios (`/api/webhooks/wapi` e `/api/webhooks/wapi-assistente`). O **canal decide, nunca o remetente**: no número da empresa todos são clientes (até funcionário vinculado); no interno só respondem funcionários verificados ou o número do dono, e qualquer outro é ignorado sem resposta. O código de verificação sai pelo número interno. No código não há canal "padrão": cada ponto de envio declara qual número usa. |
| D22 | **Docker no Mac via Colima**, não Docker Desktop (03/10) | Máquina nova (Mac Apple Silicon, 8 GB). O instalador do Docker Desktop exige `sudo` (cria `/usr/local/bin`); o Colima instala tudo pelo Homebrew sem senha e roda o mesmo `docker compose`. VM: 4 CPUs, 4 GB, 60 GB, Virtualization.Framework da Apple. Ligar/desligar: `colima start` / `colima stop` — não sobe sozinho no login (`brew services start colima` se quiser). Trocar para Docker Desktop depois não muda nada no projeto. |
| D23 | **Carrinho do caixa se adapta à própria largura** (container query), não à da janela (03/10) | Bug visto pelo usuário: "Produto" e "Preço unit." sobrepostos. Ao lado da coluna de pagamento (24rem), o carrinho fica com ~40rem mesmo em tela grande, e as colunas fixas somavam ~39rem. Três níveis: largo (tabela completa), médio (preço unitário desce para baixo do nome, sai a coluna) e estreito (sem cabeçalho, linhas empilhadas). |
| D24 | **Produto sem estoque: foto em cinza com selo "Sem estoque" centralizado** (03/10) | Pedido do usuário. Feito no ponto único que monta a foto (`criarImagemPrincipal`), vale para catálogo, Gerenciar produtos e carrinho. Usa a mesma regra de esgotado já existente (`situacaoEstoque`, estoque ≤ 0). Em miniaturas pequenas (carrinho, Gerenciar produtos) o texto não cabe — fica só o cinza. |
| D25 | **Detalhe do pedido mostra para quem foi e quem vendeu; voltar destaca o pedido na lista** (04/10) | Pedido do usuário. Abaixo do número: "Cliente: …" (ou "Consumidor não identificado") e "Vendido por: …". A venda continua guardando o **e-mail** do operador (é a identidade); o **nome** é resolvido na leitura (`UsuarioService.nomesPorEmail`, uma consulta em lote — sem N+1), então renomear um funcionário atualiza as vendas antigas. Novo campo `operadorNome` na resposta da venda. "← Voltar para Pedidos" virou botão com borda; ao voltar (botão ou voltar do navegador), a lista destaca e rola até o pedido. Abrir Pedidos pelo menu não destaca nada. |
| D26 | **Abertura e fechamento de caixa com contagem por cédula** (04/10) | Pedido do usuário para o modo supermercado: "quantas notas iniciam no balcão por dia, e notas de reposição ao longo do dia"; "o quanto entrou menos o valor inicial do dia". **Sem caixa aberto não há venda** (a venda de balcão guarda `pedidos.sessao_caixa_id`). Abertura: o operador confere, cédula por cédula, o **fundo de troco padrão** definido pelo gerente (`fundo_troco_padrao`). Durante o turno: **reposição de troco** (suprimento, por cédula) e **sangria** (valor + motivo; não pode passar do que deveria haver na gaveta). Fechamento **cego**: o operador conta sem ver o esperado; o sistema congela vendas em dinheiro, esperado (fundo + vendas em dinheiro + reposições − sangrias), contado e diferença. "Entrou no dia" = contado − fundo inicial − reposições + sangrias. Venda cancelada com o caixa dela ainda aberto sai da conta sozinha (pagamento estornado); com o caixa já fechado, vira **sangria automática** no caixa de quem cancela (exige caixa aberto). Um caixa aberto por operador (índice único parcial). Conferência de todos os caixas e o fundo padrão exigem a permissão nova **CAIXA_CONFERIR** (dada ao perfil Financeiro). |
| D27 | **Gestão de caixa separada da venda, por permissão** (04/10) | Pedido do usuário: "separar a reposição, sangria e etc do sistema de caixa"; abrir, repor e demais operações "iniciados por um cargo que tem essa permissão específica". Permissão nova **CAIXA_GERENCIAR** (dada a quem já tinha CAIXA_CONFERIR): abre o caixa **em nome de um operador** (funcionário ativo com PDV_VENDER, um caixa aberto por vez), repõe troco, faz sangria e fecha **qualquer** caixa, na tela **Gestão de caixa**. A tela do **Caixa** fica só com a venda e uma faixa informativa ("Caixa aberto às 08:00 por Fulano"); sem caixa aberto, avisa para pedir a abertura. A sessão guarda `aberta_por` e `fechada_por` (V16); o caixa continua sendo do operador (as vendas dele somam nele). CAIXA_CONFERIR segue para a conferência e o fundo de troco padrão. |

## Em aberto

| # | Pendência | Bloqueia o quê |
|---|---|---|
| A1 | Regime tributário da empresa (Simples / Presumido / Real) | Cálculo de CST/CSOSN, ICMS-ST, DIFAL (fatia 2). Não bloqueia a fatia 1; o motor de cálculo será parametrizado por regime quando chegar a hora, aceito como placeholder por ora ("primeiro pode ser", 02/10). |
| A2 | TEF (Transferência Eletrônica de Fundos — cartão físico no ponto de venda): qual provedor/SDK (PayGo Web, SiTef, outro) e se existe maquininha física de verdade no cenário | Adiado explicitamente pelo usuário em 02/10 ("vamos fazer isso posteriormente"). Nada implementado ainda — nenhum código, dependência ou endpoint relacionado a TEF existe no projeto. Ao retomar: PayGo Web é a opção mais viável de integrar e testar neste ambiente (API HTTP, encaixa no mesmo padrão adapter + outbox já usado para Mercado Pago/WhatsApp); SiTef exige cliente local (CliSiTef) e homologação própria do fornecedor, bem mais pesado. |
| A3 | **Vender a unidade ou a embalagem** (lata avulsa ou fardo com 12) | Adiado pelo usuário em 04/10 ("sem isso por enquanto"). Proposta pronta: entidade **Embalagem** dentro do Produto (nome, código de barras próprio, quantas unidades contém, preço fixo); estoque sempre em unidades (vender 1 fardo baixa 12); o leitor procura o código no produto e nas embalagens; na NF-e/NFC-e, unidade comercial (fardo) separada da tributável (lata — `uCom`/`qCom` × `uTrib`/`qTrib`, `cEANTrib`). Não confundir com promoção "leve 3 pague 2" (regra de preço, item próprio da fila). Ao retomar, perguntar: o fardo tem código de barras próprio? |

## Raciocínio: anexos de WhatsApp (D8 — decidido)

**A regra:** "todo estado fica no PostgreSQL... o backup diário cobre apenas o banco."

**Por que isso tensiona com anexos de mídia:**
- Guardar imagem/áudio/PDF como `bytea` cumpre a letra da regra, mas infla o banco com dados binários
  que crescem sem parar — WAL, backup e replicação ficam mais pesados a cada mensagem, e o Postgres
  não foi pensado para servir blob em volume.
- Guardar em object storage (MinIO, self-hosted, roda em Docker como tudo mais) resolve isso, mas aí
  os bytes da mídia ficam fora do banco — a regra existe justamente para que nada relevante fique sem
  backup.

**Minha leitura:** a regra protege **estado de negócio**, não qualquer byte que passa pelo sistema. Separar:
- **No Postgres (fonte da verdade):** quem mandou, quando, tipo MIME, tamanho, checksum, e a referência
  ao objeto — isso é o que a regra de negócio e a auditoria (RF08/RNF04) realmente usam.
- **No MinIO:** só os bytes da mídia, com **sua própria política de backup** (não "sem backup" — uma
  política explícita, dimensionada ao conteúdo, e não a `bytea` escondido dentro do backup do banco).

Isso não é abstração prematura (YAGNI não se aplica: RF01 já exige anexos agora), é tratar dois tipos de
dado com características diferentes de forma diferente.

**Decidido:** separar — metadados no Postgres, bytes no MinIO.

## Mercado Pago: webhook e validação

1. **Nunca confiar no payload do webhook.** Ele só avisa que algo mudou (`topic` + `id`). Ao receber,
   buscar o recurso de verdade via `GET /v1/payments/{id}` com o access token antes de agir.
2. **Validar a assinatura:** cabeçalhos `x-signature` e `x-request-id`; a assinatura é um HMAC-SHA256
   calculado com o segredo configurado no painel do Mercado Pago. Requisição sem assinatura válida é
   descartada sem processar.
3. **Idempotência:** guardar o `id` da notificação já processada (tabela de eventos) — o Mercado Pago
   reenvia em caso de timeout, e sem isso a baixa de cobrança duplica.
4. **Responder rápido:** o webhook só enfileira o evento (outbox do Postgres, já decidido para RNF05) e
   devolve 200/201 na hora. O processamento (baixa, emissão de nota) acontece depois, assíncrono.
5. **Reconciliação:** job periódico comparando pedidos com pagamento pendente contra o status real na
   API do Mercado Pago, para cobrir qualquer webhook perdido.

## Fatia 1 (Fundação) — construída em 2026-10-02

Backend (`backend/`): autenticação JWT, CRUD de usuários com perfil (RF09), CRUD de clientes com
`Cpf`/`Cnpj` como Value Objects (RF04), tratamento de erro centralizado, Flyway baseline, Docker
multi-stage. Frontend (`frontend/`): TS puro/strict, módulos ES nativos (sem bundler), tela de
login. Detalhes e pendências em [README.md](README.md) e [docs/MODELO-DOMINIO.md](docs/MODELO-DOMINIO.md).

## Fatia 2 (Núcleo fiscal, parcial) — construída em 2026-10-02

Produto (NCM), Servico (código LC 116/2003 + alíquota de ISS validada entre 2%-5%), Pedido como
agregado raiz com `ItemPedido` (preço e descrição congelados no momento da compra, via
`@ElementCollection`) e máquina de estado própria (ABERTO → AGUARDANDO_EMISSAO → CONCLUIDO |
CANCELADO). `Dinheiro` como Value Object monetário (nunca `float`), convertido via
`AttributeConverter` com `autoApply=true`. `DocumentoFiscal` modelado (NFE/NFSE, ciclo de vida
PENDENTE → AUTORIZADO/REJEITADO → CANCELADO) mas **sem emissão real ligada** — isso é o próximo
passo depois que regime tributário e credenciais do provedor existirem.

## Fatia 3 (Financeiro) — construída em 2026-10-02

`Cobranca` (Pix/boleto) ligada a um Pedido `AGUARDANDO_EMISSAO`. `ProvedorPagamento` como porta
(hexagonal) com `MercadoPagoProvedorPagamento` como adapter real via `java.net.http.HttpClient`
(sem nova dependência). Webhook implementado exatamente como documentado na seção "Mercado Pago —
webhook e validação" acima: `MercadoPagoWebhookController` (endpoint público, validado por HMAC)
só grava um evento outbox (`EventoWebhookPagamento`) e responde na hora; `WebhookPagamentoProcessor`
(`@Scheduled`, a cada 10s) processa de forma assíncrona, sempre confirmando o status via
`consultarStatus` antes de dar baixa na cobrança — nunca confia no payload recebido.

Partes com confiança média, marcadas em código e em PENDENCIAS.md: o corpo exato da API de
pagamentos do Mercado Pago e o formato da assinatura do webhook seguem a documentação pública mas
nunca foram testados contra a API real (não há credenciais de sandbox nesta máquina).

## Fatia 4 (Atendimento) — construída em 2026-10-02

`Conversa` identificada pelo **telefone**, não pelo Cliente — a primeira mensagem de WhatsApp
chega antes de qualquer cadastro existir, e RF04 exige CPF/CNPJ no Cliente. Ao criar, vincula
automaticamente a um Cliente já cadastrado com aquele telefone, se existir; senão fica sem vínculo
até alguém formalizar o cadastro (vínculo retroativo não automatizado nesta fatia — gap conhecido,
não implementado por ora).

`Mensagem`/`Anexo`: metadado no Postgres, bytes do anexo no MinIO via porta `ArmazenamentoObjetos`
(decisão D8). Envio de WhatsApp via porta `WhatsAppGateway`, adapter `EvolutionApiWhatsAppGateway`
(decisão D2 — API não oficial, risco aceito). Entrada de mensagens pelo mesmo padrão outbox já
usado no financeiro: `EvolutionApiWebhookController` (endpoint público, validado por chave
compartilhada) grava o payload bruto; `WebhookWhatsAppProcessor` interpreta de forma assíncrona.

Inbox (RF07) cobre fila de conversas abertas, atribuição de atendente e troca de mensagens. Tags e
SLA (também citados em RF07) não entraram nesta fatia.

Partes com confiança baixa/média, nunca testadas (sem Docker disponível até agora): o formato
exato do payload de envio/webhook da Evolution API (projeto com várias versões incompatíveis entre
si) e a integração com o MinIO. Tudo consolidado em [PENDENCIAS.md](PENDENCIAS.md).

Não foi possível compilar/testar localmente em nenhuma das quatro fatias: esta máquina não tinha
Java, Maven, Node, Docker nem git instalados, e a instalação do Docker Desktop esbarrou em
**virtualização desligada na BIOS/UEFI** — corrigir isso é pré-requisito para rodar qualquer coisa
(ver conversa de 2026-10-02).
