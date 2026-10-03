# Contexto da conversa

Resumo do que foi construído e combinado nas conversas com o Claude Code até 03/10/2026, para
continuar o trabalho numa sessão nova (por exemplo, Claude Code na web) sem perder o fio.
Leia junto com [CLAUDE.md](CLAUDE.md) (regras do código, obrigatórias), [DECISOES.md](DECISOES.md)
(o porquê de cada escolha, D1–D21a) e [PENDENCIAS.md](PENDENCIAS.md) (o que falta testar/ligar).

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
  negativo, histórico de movimentações, custo do produto; serviços (geram NFS-e).
- **Caixa (PDV):** leitor de código de barras (USB/Bluetooth, funciona como teclado), carrinho em
  tabela com − e +, cliente opcional, CPF na nota, NFC-e, dinheiro com troco, maquininha
  integrada (Mercado Pago Point) com modo manual de contingência, Pix com QR na tela,
  cancelamento com estorno.
- **Pedidos pelo painel:** confirmação baixa o estoque e gera NF-e/NFS-e em `PENDENTE`;
  cancelamento e reembolso devolvem ao estoque.
- **Painel = central de relatórios:** filtro de período, 4 indicadores com comparação ao período
  anterior, faturamento no tempo, pizzas (rosca) para formas de pagamento, canal, custo × lucro e
  pedidos por situação; dias da semana, horário de pico, mais vendidos; exportação CSV para Excel.
- **WhatsApp (W-API):** bot de atendimento ao cliente e assistente do dono num **segundo número
  separado** (com verificação do número), para evitar vazamento de dados. Duas IAs Claude isoladas.
- **Vitrine pública** de produtos (preparação para o site de vendas).
- Interface: tela cheia, barra lateral recolhível, busca em cada tabela, botões com borda,
  destrutivos em vermelho.
- **Testes:** 159 testes unitários do backend passando.

## O que ainda não está ligado (depende do Gustavo)

- `CLAUDE_API_KEY` / `CLAUDE_API_KEY_ATENDIMENTO` no `.env` (sem elas, bot e assistente não respondem).
- Túnel público para os webhooks da W-API (confirmar antes de abrir).
- Segunda instância da W-API para o número do assistente.
- Token do Mercado Pago e ID da maquininha Point.
- Certificado A1, CSC da NFC-e e regime tributário → transmissão fiscal real (adiada), TEF (adiado).
- Cadastrar o custo dos produtos para o lucro ficar completo.

## Próximos passos combinados (supermercado), em ordem sugerida

1. **Abrir e fechar caixa:** fundo de troco, sangria, suprimento, conferência no fechamento com
   diferença. *(recomendado começar por aqui)*
2. **Venda por peso:** ler etiqueta da balança (código de barras com peso/preço embutido).
3. **Entrada de mercadoria pelo XML da nota do fornecedor** — cobre também "Contas a pagar" e a
   aba "Contatos" (fornecedores, frete).
4. **Validade e lote**, com aviso do que está perto de vencer.
5. **Promoções** (preço de oferta com período, "leve 3 pague 2").
6. **Desconto no caixa** liberado por senha do gerente.

Outras ideias que o Gustavo pediu e continuam na fila: histórico de vendas com busca por produto;
renomear "Cobranças" para "Recebimentos"; área só de financeiro com exportação; bot enviando foto
do produto no WhatsApp; site de vendas com pagamento automático (confirmar sozinho ao pagar e
reembolsar se faltar estoque).

## Como rodar e conferir

- `docker compose up -d --build` → frontend em http://localhost:8081 (nginx faz proxy de `/api/`).
- Testes do backend (sem Maven instalado na máquina):
  `docker run --rm -v "<pasta do projeto>:/app" -v projectsystem_maven_cache:/root/.m2 -w /app/backend maven:3.9-eclipse-temurin-21 mvn -B -q test`
- Logins de teste: `dono@empresax.com` (irrestrito) e `admin@empresax.com` (irrestrito, senha
  fraca — trocar antes de expor). **Senhas não ficam no repositório**: pergunte ao Gustavo ou veja o `.env`.

## Como o Gustavo prefere trabalhar

- Fala direto e informal; quer resultado visual caprichado: tela cheia, linhas e bordas
  visíveis, espaçamento uniforme, fundo não muito branco, pouca informação por tela, nada com
  "cara de IA". Conferir sempre no navegador (capturas de tela) antes de entregar.
- Segredos nunca no código, no repositório ou em resposta; o `.env` fica fora do GitHub.
- Enviar ao GitHub só quando ele pedir. Confirmar antes de ações externas ou destrutivas
  (túnel, mensagem real no WhatsApp, apagar dados).
- Se um pedido conflitar com o CLAUDE.md, explicar o conflito antes; se faltar informação, perguntar.
