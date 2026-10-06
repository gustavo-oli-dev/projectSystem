# Contexto da conversa

Resumo do que foi construído e combinado nas conversas com o Claude Code até 06/10/2026, para
continuar o trabalho numa sessão nova (por exemplo, Claude Code na web) sem perder o fio.
Leia junto com [CLAUDE.md](CLAUDE.md) (regras do código, obrigatórias), [DECISOES.md](DECISOES.md)
(o porquê de cada escolha, D1–D43 e pendências A1–A3) e [PENDENCIAS.md](PENDENCIAS.md) (o que falta
testar/ligar).

## O projeto

- Treino pessoal do Gustavo: ele foi contratado por uma empresa que precisa colocar conformidade
  fiscal (reforma tributária, LC 214/2025) em todos os seus sistemas. Este sistema de vendas da
  "Empresa X" (Fortaleza/CE) é o laboratório.
- **Foco atual (decidido em 03/10): sistema de supermercado.** Caixa com leitor de código de
  barras, NFC-e, estoque, relatórios. Serviços/NFS-e ficam em segundo plano.
- Stack fixa: Java 21 + Spring Boot 3, Maven, PostgreSQL + Flyway, TypeScript puro (sem
  framework, `strict`, sem `any`, nunca `innerHTML`), tudo em Docker Compose.

## O que já existe e funciona

- **Acesso:** login, usuário irrestrito (dono/programador) e "Perfis de acesso" com permissões
  editáveis atribuídos aos funcionários (área "Funcionários e Perfis").
- **Catálogo:** produtos com foto, código de barras (GTIN validado), estoque que nunca fica
  negativo, histórico de movimentações, custo do produto; serviços (geram NFS-e). Produto
  esgotado aparece com a **foto em cinza e o selo "Sem estoque"** (D24).
- **Caixa (PDV):** leitor de código de barras (USB/Bluetooth, funciona como teclado), carrinho em
  tabela com − e + que **se adapta à própria largura** (D23), cliente opcional, CPF na nota, NFC-e,
  dinheiro com troco, maquininha integrada (Mercado Pago Point) com modo manual de contingência,
  Pix com QR na tela, cancelamento com estorno.
- **Pedidos pelo painel:** confirmação baixa o estoque e gera NF-e/NFS-e em `PENDENTE`;
  cancelamento e reembolso devolvem ao estoque. O **detalhe do pedido** mostra no topo "Cliente:" e
  "Vendido por:" (nome, não e-mail); "← Voltar para Pedidos" destaca o pedido na lista (D25).
- **Painel = central de relatórios:** filtro de período, 4 indicadores com comparação ao período
  anterior, faturamento no tempo, pizzas (rosca) para formas de pagamento, canal, custo × lucro e
  pedidos por situação; dias da semana, horário de pico, mais vendidos; exportação CSV para Excel.
- **WhatsApp (W-API):** bot de atendimento ao cliente e assistente do dono num **segundo número
  separado** (com verificação do número), para evitar vazamento de dados. Duas IAs Claude isoladas.
- **Vitrine pública** de produtos (era preparação para o site de vendas, descartado em 06/10).
- Interface: tela cheia, barra lateral recolhível, busca em cada tabela, botões com borda,
  destrutivos em vermelho.
- **Testes:** 262 testes do backend passando (06/10).

## Sessão de 03–04/10: máquina nova (Mac) e primeira execução de verdade

- **Máquina nova:** Mac Apple Silicon (8 GB, 6 núcleos). Antes o projeto nunca tinha rodado (a
  máquina antiga, Windows, estava com a virtualização desligada na BIOS).
- **Docker via Colima** (D22): o Docker Desktop pede `sudo`; o Colima instala sem senha. Ligar com
  `colima start` — não sobe sozinho no login.
- **Primeira execução:** compila, 159 testes passando, 14 migrations aplicadas, sistema no ar em
  http://localhost:8081. Defeito achado: erro do cliente (caminho inexistente, parâmetro faltando)
  vira 500 em vez de 404/400 — está no PENDENCIAS.md.
- **`.env` local criado** com segredos gerados aleatoriamente (permissão 600, fora do git). Token do
  Mercado Pago, W-API e chaves da Claude ficaram vazios ou com valor provisório.
- **Dados de demonstração** (só no banco local, pela API, valendo as regras do domínio): 20
  produtos de supermercado com EAN-13 válido (`7891000000014` a `7891000000205`), NCM, custo e
  estoque inicial; 12 clientes (10 CPF + 2 CNPJ, dígitos válidos, nomes fictícios); fotos de
  licença aberta do Wikimedia Commons, escolhidas uma a uma, sem marca visível (créditos em
  [docs/CREDITOS-FOTOS-DEMO.md](docs/CREDITOS-FOTOS-DEMO.md)). O Gustavo fez a primeira venda no
  caixa (1 arroz, dinheiro).
- **Teste de carga** (k6 em container, pelo nginx, só leitura — 50% leitura de código de barras,
  25% lista de produtos, 15% vitrine, 10% relatório; rampa de 25 a 1600 req/s): **118 mil
  requisições, zero falhas**. Até ~1000 req/s, p95 ≤ 3 ms; a curva começa em 1200 (p95 10 ms); satura
  em **~1.400 req/s** (p95 até 186 ms). O teto é da VM (4 núcleos divididos entre backend, Postgres,
  nginx e o próprio k6), não necessariamente do sistema. Um supermercado com 10 caixas faz ~10–20
  req/s no pico. A primeira tentativa foi descartada: o Mac entrou em repouso na bateria no meio —
  repetir sempre com `caffeinate -i`. Falta o teste de **vendas simultâneas** (gasta estoque).
- **Decisões do Gustavo nesta sessão:** embalagens (vender unidade ou fardo) **adiadas** — proposta
  pronta em A3 (DECISOES.md); próximo da fila continua sendo abrir e fechar caixa.

## Sessão de 04–05/10: caixa completo e rotina de supermercado (máquina Windows)

Trabalho feito numa máquina Windows com Docker Desktop (migrations até a V23). Cada item tem a
decisão correspondente no DECISOES.md:

- **Abrir e fechar caixa** (D26–D27): fundo de troco contado por cédula, reposição e sangria,
  fechamento cego. A **"Gestão de caixa"** é separada da venda e exige uma permissão própria
  (CAIXA_GERENCIAR). Quem gerencia abre, repõe, faz sangria e fecha; o operador só vende.
- **Caixas numerados** (Caixa 01, 02, 03…): abrir vários de uma vez, com um operador por caixa. O
  fechamento confere cada forma de pagamento (crédito, débito e Pix contra o relatório da
  maquininha) e faz um resumo do dia por caixa.
- **Fechamento compara a gaveta com os produtos vendidos em dinheiro** e diz se está "certo",
  "devendo" ou "sobrando". Conta: o que entrou menos o valor inicial do dia.
- **Painel em abas:** Vendas, Produtos, Horários, Caixa, **Dinheiro do dia** e Operação.
  - "Dinheiro do dia" é a conferência do gerente: a gaveta comparada com o que o sistema registrou.
  - Tudo que é estatística vai para o relatório, com exportação CSV.
- **Perdas e quebras** com motivo, **inventário** (sobra ou falta ajusta o estoque) e relatório de
  perdas no Painel.
- **Estoque mínimo** por produto e tela **"Reposição"**, com lista de compra em CSV.
- **Compras** (novo grupo no menu):
  - **Entrada por nota**: lê o XML da NF-e do fornecedor com leitura segura contra XXE e dá entrada
    no estoque com o custo.
  - **Contatos**: fornecedores e frete.
  - **Contas a pagar**.
- **Desconto e cancelamento de item no caixa com senha do gerente** (D35, permissão PDV_AUTORIZAR):
  - A senha gera um token de 5 minutos, preso à ação e ao operador. Ele não serve como login.
  - O desconto é rateado por item.
  - Os relatórios descontam o desconto.
  - Item cancelado fica registrado no Painel.
- **Pagamento dividido** (D36):
  - "Dividir em mais de uma forma" lança até 5 partes já recebidas (dinheiro, ou cartão/Pix na
    maquininha com o comprovante).
  - O restante fecha a venda do jeito normal: dinheiro com troco, maquininha ou contingência.
  - Pix com QR na tela não entra no dividido.
  - A conferência da gaveta conta só a parte paga em dinheiro.
- **Decisão do Gustavo:** **venda por peso descartada por enquanto**. A balança imprime a etiqueta
  e o caixa só lê o código de barras.

## Sessão de 06/10: "faça todos, menos o site"

O Gustavo pediu para fazer tudo o que estava na fila, menos o site (descartado). Cada item tem a
decisão no DECISOES.md (D37–D43); migrations até a V28; 262 testes do backend passando.

- **Retirada de produtos** (D37): tela própria, lê o código de barras, quantidade e motivo da lista
  ou escrito. A **validade** funciona assim, por decisão dele: retirar a quantidade com o motivo,
  sem lote nem data de validade. A **troca** também é retirada: motivo "Troca com cliente" (o
  produto novo sai do estoque). A vitrine pública `/api/loja/produtos` foi removida.
- **Promoções** (D38): preço de oferta e "leve X pague Y" com período, aplicadas sozinhas na venda.
  O carrinho mostra o selo e o preço certo; o recibo mostra "Promoções − R$ X".
- **Preços e etiquetas** (D39): reajuste em lote (percentual ou preço único) com histórico de preços,
  e etiquetas de gôndola para imprimir, com código de barras EAN-13 desenhado.
- **Financeiro e ajustes** (D40):
  - área "Financeiro" (entrou, saiu, saldo e a pagar, dia a dia, com CSV);
  - coluna "Maquininha" no Painel → Caixa;
  - Pedidos com Cliente, Canal e Produtos (a busca acha vendas pelo produto);
  - "Cobranças" virou "Recebimentos".
- **Embalagens** (D41): vender a unidade ou o fardo, com código de barras próprio opcional; o
  estoque continua em unidades (1 fardo de 12 baixa 12).
- **Tributação por produto** (D42): CST ou CSOSN, origem, ICMS, substituição tributária com CEST,
  cesta básica e cClassTrib do IBS/CBS. A transmissão da NFC-e continua dependendo do certificado.
- **Foto do produto pelo bot do WhatsApp** (D43): ainda não testada contra a W-API real.

## O que ainda não está ligado (depende do Gustavo)

- `CLAUDE_API_KEY` / `CLAUDE_API_KEY_ATENDIMENTO` no `.env` (sem elas, bot e assistente não respondem).
- Túnel público para os webhooks da W-API (confirmar antes de abrir).
- Segunda instância da W-API para o número do assistente.
- Token do Mercado Pago e ID da maquininha Point.
- Certificado A1, CSC da NFC-e e regime tributário → transmissão fiscal real (adiada), TEF (adiado).
- Cadastrar o custo dos produtos reais para o lucro ficar completo (os de demonstração já têm).

## Próximos passos

A fila combinada até 06/10 está toda feita (menos o site, descartado). O que resta depende do
Gustavo (lista acima: chaves da Claude, W-API, Mercado Pago, certificado e regime tributário) ou
de uma decisão nova dele. Ideias naturais para a próxima rodada:

- validar NCM × CEST × cClassTrib contra as tabelas oficiais;
- gerar o XML da NFC-e quando o certificado chegar (usa a tributação do D42 e as embalagens do D41);
- contar fardos em unidades nos relatórios de "mais vendidos".

## Como rodar e conferir

- No Mac: `colima start`. No Windows: abrir o Docker Desktop. Depois `docker compose up -d --build`
  → frontend em http://localhost:8081 (o nginx faz proxy de `/api/`). Logo depois do build o login
  pode dar 504 enquanto o backend sobe; espere a resposta 401 em `/api/caixa/atual`.
- Testes do backend (sem Maven instalado na máquina):
  `docker run --rm -v "<pasta do projeto>:/app" -v projectsystem_maven_cache:/root/.m2 -w /app/backend maven:3.9-eclipse-temurin-21 mvn -B -q test`
- Checagem de tipos do frontend:
  `docker run --rm -v "<pasta>/frontend:/app" -w /app node:22-alpine sh -c "npm install && npx tsc --noEmit -p ."`
  (apague o `package-lock.json` que o `npm install` cria — o projeto não usa lockfile por enquanto).
- Login neste Mac: `dono@empresax.com` (irrestrito, criado a partir do `.env`). O
  `admin@empresax.com` **não existe aqui** — ficou no banco da máquina antiga; recriar por
  "Funcionários e Perfis" se quiser. **Senhas não ficam no repositório nem nas respostas**: estão
  no `.env` (`ACESSO_INICIAL_SENHA`).

## Como o Gustavo prefere trabalhar

- Fala direto e informal; quer resultado visual caprichado: tela cheia, linhas e bordas
  visíveis, espaçamento uniforme, fundo não muito branco, pouca informação por tela, nada com
  "cara de IA". Conferir sempre no navegador (capturas de tela) antes de entregar.
- Manda capturas de tela quando algo está "bugado" — olhar a imagem com atenção para achar o
  defeito exato.
- Segredos nunca no código, no repositório ou em resposta; o `.env` fica fora do GitHub. Quando
  ele pedir a senha, copiar para a área de transferência (`pbcopy` no Mac, `Set-Clipboard` no
  Windows) em vez de escrever na resposta.
- Enviar ao GitHub só quando ele pedir. Confirmar antes de ações externas ou destrutivas
  (túnel, mensagem real no WhatsApp, apagar dados). Não criar vendas ou dados que distorçam os
  relatórios sem perguntar.
- Se um pedido conflitar com o CLAUDE.md, explicar o conflito antes; se faltar informação, perguntar.
