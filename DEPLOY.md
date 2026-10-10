# Deploy: backend no Render, frontend no Netlify

Decisão D48 (DECISOES.md). O backend (Spring Boot + Postgres) vai para o **Render**; o frontend
(estático, sem build tool) vai para o **Netlify**. Os dois ficam em domínios diferentes — o que
liga um ao outro é a URL absoluta da API (`API_BASE_URL`, lida em tempo de execução por
`runtime-config.js`) e o CORS do backend (`CORS_ORIGENS_PERMITIDAS`).

A ordem importa: o backend precisa existir primeiro (o Netlify precisa da URL dele), e o
`CORS_ORIGENS_PERMITIDAS` só pode ser preenchido depois que o Netlify existir.

## Pré-requisito

Este repositório no GitHub, com os commits mais recentes enviados (`git push`). Render e Netlify
leem direto do GitHub.

## Passo 1 — Backend + banco no Render

1. Crie uma conta em [render.com](https://render.com) (dá para entrar com a conta do GitHub).
2. **New +** → **Blueprint** → conecte este repositório. O Render lê o `render.yaml` da raiz
   sozinho e propõe dois recursos: o banco `projectsystem-db` e o serviço `projectsystem-backend`.
3. Antes de confirmar, o Render pede as variáveis marcadas `sync: false` no `render.yaml`. Pelo
   menos preencha:
   - `ACESSO_INICIAL_EMAIL` e `ACESSO_INICIAL_SENHA` — o primeiro login (irrestrito).
   - Deixe `CORS_ORIGENS_PERMITIDAS` em branco por enquanto (volta nisso no Passo 4).
   - As demais (Mercado Pago, W-API, Claude, fiscal) podem ficar em branco: o sistema sobe sem
     elas, essas integrações é que ficam desligadas até você preencher (mesmo comportamento de
     hoje em local/Docker).
4. Confirme. O Render builda a imagem do `backend/Dockerfile`, sobe o Postgres gerenciado e roda
   as migrations do Flyway no primeiro boot.
5. Quando o deploy terminar, copie a URL pública do serviço (algo como
   `https://projectsystem-backend.onrender.com`). Teste:
   `https://SUA-URL.onrender.com/api/saude` deve responder `{"status":"ok"}`.

**Plano gratuito do Render:** o banco free expira depois de um tempo (confira o prazo atual no
próprio painel antes de confiar nele além de teste); o serviço web gratuito "dorme" sem uso e leva
uns 30–60s para acordar na primeira requisição depois de um tempo parado. Para algo permanente,
troque os planos `free` do `render.yaml` por um pago depois que tudo estiver validado.

## Passo 2 — Frontend no Netlify

1. Crie uma conta em [netlify.com](https://netlify.com) (também dá para entrar com o GitHub).
2. **Add new site** → **Import an existing project** → conecte o mesmo repositório. O Netlify lê o
   `netlify.toml` da raiz sozinho (base `frontend/`, comando de build, pasta publicada).
3. Antes do primeiro deploy, em **Site configuration → Environment variables**, adicione:
   - `API_BASE_URL` = a URL do backend do Passo 1 (ex.: `https://projectsystem-backend.onrender.com`,
     **sem** `/api` no final — o código já completa isso).
4. Deploy. Quando terminar, copie a URL do site (algo como `https://seu-site.netlify.app`).

## Passo 3 — Fechar o CORS

Volte no Render, no serviço `projectsystem-backend` → **Environment**, e preencha
`CORS_ORIGENS_PERMITIDAS` com a URL do Netlify do Passo 2 (ex.:
`https://seu-site.netlify.app` — sem barra no final). O Render reinicia o serviço sozinho. Dá para
colocar mais de uma origem separando por vírgula (ex.: domínio próprio + domínio do Netlify).

Sem esse passo o navegador bloqueia as chamadas da API com erro de CORS — o backend funciona
normalmente por `curl`/Postman, só o navegador que recusa.

## Depois do ar

- Abra a URL do Netlify, faça login com o `ACESSO_INICIAL_EMAIL`/`ACESSO_INICIAL_SENHA` do Passo 1.
- Se der "Não foi possível conectar ao servidor": confira se `API_BASE_URL` no Netlify está certo
  (sem `/api`, sem barra no final) e se `CORS_ORIGENS_PERMITIDAS` no Render bate exatamente com a
  URL do Netlify (schema + domínio, sem caminho).
- O serviço do Render no plano free dorme: a primeira requisição depois de um tempo parado demora.

## O que fica de fora deste deploy

- **MinIO** (anexos de WhatsApp): não entra no `render.yaml` — não há um plano gerenciado
  equivalente gratuito no Render. `MinioArmazenamentoObjetos` já é não-fatal ao não encontrar o
  serviço (ver a classe); só os anexos de WhatsApp ficam fora do ar. Se precisar, um S3-compatível
  externo (Cloudflare R2, Backblaze B2) resolve — é só apontar `MINIO_ENDPOINT` para lá.
- **Webhooks do Mercado Pago e da W-API**: dependem de uma URL pública para o provedor chamar de
  volta — a URL do Render já serve para isso (`https://SUA-URL.onrender.com/api/webhooks/...`),
  sem precisar de túnel separado como em desenvolvimento local.
- **Certificado A1 / transmissão de NFC-e**: nada muda aqui — continua dependendo do certificado,
  como em DECISOES.md e PENDENCIAS.md.

## Variáveis de cada lado

| Onde | Variável | Para quê |
|---|---|---|
| Render (backend) | `CORS_ORIGENS_PERMITIDAS` | Origens que podem chamar a API (a URL do Netlify) |
| Render (backend) | `PORT` | Definida pelo próprio Render; o backend já lê `${PORT:8080}` |
| Netlify (frontend) | `API_BASE_URL` | URL do backend, sem `/api` — vira `runtime-config.js` no build |

Em Docker Compose local nenhuma das duas precisa ser definida: o nginx do frontend faz proxy de
`/api` para o backend, então é tudo a mesma origem (comportamento inalterado).
