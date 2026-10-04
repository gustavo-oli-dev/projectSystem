# Contexto da conversa

Resumo do que foi construído e combinado nas conversas com o Claude Code até 04/10/2026, para
continuar o trabalho numa sessão nova (por exemplo, Claude Code na web) sem perder o fio.
Leia junto com [CLAUDE.md](CLAUDE.md) (regras do código, obrigatórias), [DECISOES.md](DECISOES.md)
(o porquê de cada escolha, D1–D25 e pendências A1–A3) e [PENDENCIAS.md](PENDENCIAS.md) (o que falta
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
- **Vitrine pública** de produtos (preparação para o site de vendas).
- Interface: tela cheia, barra lateral recolhível, busca em cada tabela, botões com borda,
  destrutivos em vermelho.
- **Testes:** 159 testes unitários do backend passando.

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

## O que ainda não está ligado (depende do Gustavo)

- `CLAUDE_API_KEY` / `CLAUDE_API_KEY_ATENDIMENTO` no `.env` (sem elas, bot e assistente não respondem).
- Túnel público para os webhooks da W-API (confirmar antes de abrir).
- Segunda instância da W-API para o número do assistente.
- Token do Mercado Pago e ID da maquininha Point.
- Certificado A1, CSC da NFC-e e regime tributário → transmissão fiscal real (adiada), TEF (adiado).
- Cadastrar o custo dos produtos reais para o lucro ficar completo (os de demonstração já têm).

## Próximos passos combinados (supermercado), em ordem sugerida

1. ~~**Abrir e fechar caixa**~~ — **feito em 04/10 (D26)**: fundo de troco por cédula, reposição,
   sangria, fechamento cego e tela "Conferência de caixa".
2. **Venda por peso:** ler etiqueta da balança (código de barras com peso/preço embutido).
3. **Entrada de mercadoria pelo XML da nota do fornecedor** — cobre também "Contas a pagar" e a
   aba "Contatos" (fornecedores, frete).
4. **Validade e lote**, com aviso do que está perto de vencer.
5. **Promoções** (preço de oferta com período, "leve 3 pague 2").
6. **Desconto no caixa** liberado por senha do gerente.

Outras ideias que o Gustavo pediu e continuam na fila: histórico de vendas com busca por produto;
renomear "Cobranças" para "Recebimentos"; área só de financeiro com exportação; bot enviando foto
do produto no WhatsApp; site de vendas com pagamento automático (confirmar sozinho ao pagar e
reembolsar se faltar estoque); embalagens unidade/fardo (A3, adiado em 04/10); coluna "Cliente" na
lista de Pedidos (oferecida em 04/10, ainda sem resposta).

## Como rodar e conferir

- No Mac: `colima start`, depois `docker compose up -d --build` → frontend em
  http://localhost:8081 (nginx faz proxy de `/api/`).
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
  ele pedir a senha, copiar para a área de transferência (`pbcopy`) em vez de escrever na resposta.
- Enviar ao GitHub só quando ele pedir. Confirmar antes de ações externas ou destrutivas
  (túnel, mensagem real no WhatsApp, apagar dados). Não criar vendas ou dados que distorçam os
  relatórios sem perguntar.
- Se um pedido conflitar com o CLAUDE.md, explicar o conflito antes; se faltar informação, perguntar.
