---
name: springboot-security
description: Checklist de segurança Spring deste projeto — JWT stateless, endpoints autenticados por padrão, Bean Validation, CORS por variável, segredos fora do código, erros sem vazamento. Use ao criar/alterar endpoints, autenticação, permissões, webhooks, upload ou configuração de segurança.
metadata:
  origin: Adaptado de ECC (affaan-m/ECC, MIT) para os fundamentos do CLAUDE.md
---

# Segurança Spring Boot

O `CLAUDE.md` prevalece sobre esta skill. Configuração central: `config/SecurityConfig.java`.

## Autenticação

- JWT stateless no header `Authorization: Bearer` (`JwtAutenticacaoFiltro`). CSRF fica desligado
  porque não há cookie de sessão. Se algum dia entrar cookie, CSRF volta.
- Endpoint novo é autenticado por padrão. Público só entra em `ENDPOINTS_PUBLICOS` com um comentário
  justificando (login, saúde, webhook com token próprio).
- Webhook (Mercado Pago, W-API) valida o segredo/token do provedor antes de qualquer processamento.
- Senha só com o `PasswordEncoder` bean, nunca hash manual.

## Autorização

- Permissão checada no backend em toda operação sensível; esconder botão no frontend não é controle.
- Negar por padrão; conceda o mínimo para o cargo.

## Entrada

- Todo request body é record DTO com Bean Validation (`@NotBlank`, `@Size`, `@Email`, `@Positive`...)
  e `@Valid` no controller.
- Regra de negócio continua na entidade (`DomainException`); Bean Validation cobre só o formato.
- Queries só parametrizadas (Spring Data ou JPQL com `:parametro`).

## Upload de arquivo

- Valide tamanho, content type e extensão na fronteira; nunca confie no nome enviado pelo cliente.

## CORS

- Origens vêm de `CORS_ORIGENS_PERMITIDAS` (variável de ambiente). Nunca `*`, nunca URL fixa no código.

## Segredos e logs

- Nenhum segredo em código, `application.yml`, Dockerfile, `render.yaml` (use `sync: false`) ou log.
- Não logar token, senha, chave de API, dados de cartão nem o corpo inteiro de webhook.

## Erros

- Tudo passa pelo `GlobalExceptionHandler` (`common/web`), com a resposta padronizada
  (`timestamp`, `status`, `mensagem`). Nenhum stack trace ou detalhe interno chega ao cliente.
- Filtro que precisa recusar request não escreve JSON à mão: delega para o tratamento padrão.

## Rate limiting

- Ainda não existe. Antes de adicionar (ex.: Bucket4j no login), justifique a dependência, como
  pede o `CLAUDE.md`.

## Checklist antes de entregar

- [ ] Endpoint novo autenticado (ou público justificado)
- [ ] Permissão verificada no service/controller
- [ ] DTO com Bean Validation e `@Valid`
- [ ] Nenhuma query concatenada
- [ ] Nenhum segredo commitado ou logado
- [ ] Erro novo mapeado no `GlobalExceptionHandler` sem vazar detalhe interno
