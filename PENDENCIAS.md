# Pendências de Validação e Teste

Lista única do que precisa ser conferido/testado antes de produção. Consolidado para fazer tudo de
uma vez depois, em vez de parar o desenvolvimento a cada item. Não apagar itens concluídos —
marcar como feito.

- [ ] **Conectar a W-API de verdade**: preencher `WAPI_INSTANCE_ID` e `WAPI_TOKEN` no `.env`, e
      cadastrar no painel da W-API a URL `https://SEU-ENDERECO/api/webhooks/wapi?token=<WAPI_WEBHOOK_TOKEN>`.
      A W-API é hospedada: ela precisa alcançar o backend por um endereço **público** — em
      desenvolvimento, isso exige um túnel (ex.: Cloudflare Tunnel/ngrok) apontando pra porta 8081.
- [ ] **Formato do payload do webhook da W-API**: não está na OpenAPI oficial. O parser
      (`InterpretadorWebhookWApi`) usa o formato usual (`sender.id`, `msgContent.conversation`,
      `fromMe`, `isGroup`). Conferir com a primeira mensagem real — o payload bruto fica em
      `eventos_webhook_whatsapp.payload_bruto`. Atenção também aos identificadores **@lid** do
      WhatsApp (a doc da W-API tem uma seção sobre isso): se o remetente vier como @lid em vez do
      telefone, o vínculo conversa ↔ cliente pelo telefone não funciona.
- [ ] **Bot de atendimento**: definir `CLAUDE_API_KEY_ATENDIMENTO` (chave separada da do gestor).
      Sem ela, toda mensagem recebida vai direto para a fila humana com o motivo "Bot indisponível".
- [x] ~~Rodar os testes do backend~~ — **feito em 2026-10-02**: 93 testes unitários, 0 falhas
      (`mvn test` num container Maven), incluindo cargos/permissões. Ainda faltam os testes de
      integração com Testcontainers.
- [ ] **Definir o e-mail de acesso irrestrito** (`ACESSO_INICIAL_EMAIL` / `ACESSO_INICIAL_SENHA` no
      `.env`) para o dono e para você. Hoje só existe `admin@empresax.com` (senha fraca de teste,
      convertido para irrestrito pela V7) — desativar ou trocar a senha antes de expor o sistema.
- [ ] **Próximas áreas pedidas (03/10)**: histórico de vendas com busca por produto; relatórios
      (quantidade vendida por dia/semana/mês/3 meses/ano, faturamento, mais vendidos, estoque
      baixo/parado, reembolsos); exportação CSV; aba Contatos (fornecedores, frete...);
      Recebimentos (renomear a aba Cobranças de clientes) e Contas a pagar + app de escaneamento.
- [ ] **Mercado Pago — reembolso e cancelamento (D18)**: `POST /v1/payments/{id}/refunds` e
      `PUT /v1/payments/{id}` com status cancelled seguem a documentação, mas não foram testados
      contra a API real (falta token de teste).
- [ ] **Site de vendas — pagamento automático**: quando o pagamento for aprovado, confirmar o pedido
      sozinho (baixa no estoque) e, se o estoque tiver acabado nesse meio-tempo, reembolsar
      automaticamente. Hoje o painel exige confirmar antes de cobrar.
- [ ] **Reembolso depois da nota emitida** (pedido CONCLUIDO): exige nota de devolução/cancelamento
      da NF-e — bloqueado até a transmissão fiscal existir.
- [ ] **Pedidos confirmados antes do estoque existir (V11)**: se cancelados agora, devolvem ao estoque
      unidades que nunca tinham saído. Só afeta os dados de demonstração.
- [ ] **Segundo número (D17)**: criar uma segunda instância na W-API com outro chip/número,
      preencher `WAPI_ASSISTENTE_INSTANCE_ID` e `WAPI_ASSISTENTE_TOKEN` no `.env` e cadastrar o
      webhook `.../api/webhooks/wapi-assistente?token=<WAPI_ASSISTENTE_WEBHOOK_TOKEN>`.
- [ ] **Assistente pelo WhatsApp (D16) — teste real**: falta (1) `CLAUDE_API_KEY` no `.env`, sem
      ela o assistente não responde nem no painel; (2) túnel público para o webhook da W-API, sem
      ele as mensagens recebidas não chegam ao sistema (o envio do código já funciona); (3) conferir
      se o remetente chega no mesmo formato do número verificado (nono dígito / @lid).
- [ ] **Testes de integração das regras de acesso** (Testcontainers): hoje a anti-escalada e os 403
      foram validados por script contra o sistema rodando (13 cenários, todos ok em 02/10), não por
      teste automatizado.
- [ ] **Compilar e rodar pela primeira vez.** Todo o código (fatias 1 e 2 — fundação e núcleo
      fiscal parcial) foi escrito sem acesso a Java/Maven/Node/Docker/git nesta máquina — nunca foi
      compilado. Rodar `mvn test` no backend, `npm run build` no frontend, e
      `docker compose up --build` de ponta a ponta.
- [ ] **CNPJ alfanumérico** (`backend/.../shared/documento/Cnpj.java`): o dígito verificador segue
      a especificação pública da Receita Federal (IN RFB 2.229/2024), mas não foi conferido contra
      o validador oficial nem contra uma biblioteca homologada.
- [ ] **Testes de integração com Testcontainers** (repositórios e controllers de Usuario, Cliente,
      Produto, Servico, Pedido, DocumentoFiscal) — só existem testes unitários de domínio até
      aqui, sem subir contexto Spring nem Postgres real.
- [ ] **Transmissão fiscal real**: a geração dos documentos já existe (aba Fiscal · SEFAZ —
      pedido confirmado gera NF-e e/ou NFS-e em `PENDENTE`, testado de ponta a ponta em
      2026-10-02). Falta a transmissão à SEFAZ-CE/Prefeitura que leva `PENDENTE` →
      `AUTORIZADO`/`REJEITADO`. Bloqueada por: certificado A1, regime tributário (A1 em
      DECISOES.md) e token do provedor de NFS-e — a aba mostra quais destes faltam.
- [ ] **Listener do resumo do wizard acumula**: cada visita à tela "Novo pedido" registra um novo
      ouvinte em `pedidoWizardState` sem remover o anterior (vazamento pequeno, não visível). O
      router não tem ciclo de desmontagem de tela; resolver quando houver um.
- [ ] **Integração real com o Mercado Pago** (`MercadoPagoProvedorPagamento`): o corpo da
      criação de pagamento (`payer.identification` em vez de `payer.email`) e os caminhos lidos na
      resposta (`point_of_interaction.transaction_data.qr_code` para Pix;
      `barcode.content`/`transaction_details.external_resource_url` para boleto) são uma
      estimativa baseada na documentação pública — nunca testados contra a API real. Fazer uma
      chamada de teste com credenciais de sandbox antes de produção.
- [ ] **Formato da assinatura do webhook do Mercado Pago** (`AssinaturaMercadoPagoValidador`):
      cabeçalho `x-signature` (`ts=...,v1=...`) e manifesto `id:...;request-id:...;ts:...;` seguem
      a documentação pública, mas nunca foram conferidos contra uma notificação real.
- [ ] **Integração real com a Evolution API** (`EvolutionApiWhatsAppGateway`,
      `WebhookWhatsAppProcessor`, serviço `evolution-api` no docker-compose): endpoint de envio,
      formato do payload do webhook (aninhamento de `data`, `key.remoteJid`,
      `message.conversation`) e as variáveis de ambiente do container são uma estimativa — a
      Evolution API tem várias versões com formatos diferentes entre si. Conferir contra a
      instância que for efetivamente usada.
- [ ] **MinIO** (`MinioArmazenamentoObjetos`): nunca testado contra uma instância real (sem
      Docker disponível até agora). Conferir criação do bucket e upload/download de um arquivo
      de verdade.
- [x] ~~Frontend sem proxy para a API~~ — **corrigido em 2026-10-02**: nginx servia os
      estáticos mas não tinha `location /api/` configurado, então toda chamada do frontend
      (incluindo login) caía em 404 do próprio nginx. Adicionado `frontend/nginx.conf` com proxy
      para `http://backend:8080/api/`. Também corrigida a mensagem de erro do login, que mostrava
      "credenciais inválidas" para qualquer falha (inclusive essa), não só para 401 de verdade.
- [x] ~~Imagem Docker do Evolution API~~ — **confirmado quebrado em 2026-10-02**:
      `atendai/evolution-api:latest` não existe ("pull access denied... repository does not
      exist"). Serviço movido para o profile opcional `whatsapp` no compose pra não travar o
      resto. Achar o nome/tag correto da imagem antes de testar a fatia de Atendimento de ponta a
      ponta.
- [ ] **`Dinheiro` dentro de `ItemPedido` (`@Embeddable` em `@ElementCollection`)**: o
      `AttributeConverter` com `autoApply=true` deveria se aplicar normalmente dentro do
      embeddable, mas esse round-trip específico (coleção de embeddable com atributo convertido)
      não foi exercitado — confirmar com um teste de integração real.
- [ ] **Migração de Fortaleza para o padrão nacional de NFS-e** (LC 214/2025): confirmar o estágio
      atual junto à SEFIN Fortaleza ou ao provedor fiscal escolhido antes de implementar a
      integração de NFS-e.
- [ ] **Regime tributário da empresa** (Simples/Presumido/Real) — ainda placeholder (pendência A1
      em DECISOES.md). Necessário antes de implementar o motor de cálculo de tributos sobre
      Produto/Servico.
