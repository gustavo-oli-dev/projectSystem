# Fundamentos do Projeto

Diretrizes arquiteturais e boas práticas de engenharia. **Estas regras são os pilares do código.
Siga-as em toda alteração. Se uma tarefa exigir quebrar alguma delas, pare e explique o porquê
antes de implementar. Nunca quebre uma regra em silêncio.**

## Stack tecnológica (imutável)

| Camada | Definição |
| --- | --- |
| Backend | Java 21 + Spring Boot 3 (versão compatível com Java 21) |
| Build | Maven. Não introduza Gradle nem dependências sem justificar a necessidade |
| Banco de dados | PostgreSQL |
| Frontend | TypeScript puro, sem framework de UI. `strict` ligado. `any` é proibido salvo justificativa em comentário |
| Execução | Tudo roda em Docker / Docker Compose |

## Boas práticas de código

- **Nomes revelam intenção:** variáveis, métodos e classes dizem o que fazem. Sem abreviações
  obscuras, sem `data`, `temp`, `aux`, `manager` genéricos.
- **Responsabilidade única:** funções pequenas, com uma única responsabilidade. Se precisar de "e"
  para descrever o que faz, divida.
- **Sem duplicação:** duplicação acidental de poucas linhas é aceitável; abstração prematura não.
- **KISS e YAGNI:** implemente o que foi pedido, da forma mais simples que funcione. Nada de código
  "para o futuro".
- **Guard clauses:** retorno antecipado em vez de `if`s aninhados.
- **Sem valores mágicos:** sem números e strings mágicos; use constantes ou enums com nome.
- **Imutabilidade por padrão:** `final` em Java, `const` e `readonly` em TypeScript. Mutabilidade só
  quando necessária.
- **Estado válido:** objetos nunca existem em estado inválido — valide ao criar. O construtor é o
  guardião do estado.
- **Retornos de coleção:** nunca retorne `null` para coleções; retorne coleção vazia. Use `Optional`
  para ausência de valor em retornos.
- **Tratamento de erro explícito:** nunca engula exceção (catch vazio ou só com log). Use exceções
  significativas.

## Orientação a objetos

- **Modelo de domínio rico:** evite modelos anêmicos (classes que são só "sacos de dados" com
  getters e setters). Regras de negócio e validações que dependem exclusivamente do estado da
  entidade ficam nela, não vazadas na camada de Service.
- **Encapsulamento forte ("Tell, Don't Ask"):** esconda detalhes de implementação. Em vez de pedir
  os dados de um objeto para decidir externamente, mande o objeto executar a ação.
- **Proteja coleções internas:** nunca retorne a referência direta de uma coleção mutável de dentro
  de um objeto. Retorne uma cópia ou uma view imutável (ex.: `Collections.unmodifiableList`).
- **Composição sobre herança:** evite hierarquias profundas. Prefira compor pequenos objetos
  focados.
- **Polimorfismo no lugar de `if`/`switch`:** se um bloco de decisão verifica repetidamente o estado
  ou tipo de um objeto para mudar comportamento, substitua por polimorfismo (interfaces/strategy),
  respeitando o limite do KISS.
- **Separação clara de DTOs:** DTOs (records do Java) existem apenas para transporte de dados nas
  fronteiras da API e não possuem comportamento de negócio.

## Backend (Java/Spring)

- **Controllers:** só recebem, validam e delegam. Regra de negócio não fica em controller.
- **Injeção de dependência:** sempre por construtor, nunca por campo (`@Autowired` em atributo é
  proibido).
- **Entidades e DTOs:** entidades JPA não saem da camada de serviço. A API expõe exclusivamente DTOs
  (records do Java 21).
- **Tratamento de erros:** centralizado (`@ControllerAdvice`) com respostas de erro padronizadas.
- **Transações:** `@Transactional` na camada de serviço, com escopo mínimo.

## Banco de dados

- **Migrations:** toda mudança de schema passa por migration versionada (Flyway). Nunca use
  `ddl-auto=update` fora de testes locais.
- **Queries seguras:** apenas queries parametrizadas. Concatenar SQL é estritamente proibido.
- **Performance:** evite N+1 — verifique sempre as queries geradas pelo JPA em relações.
- **Estado persistente:** todo estado fica no PostgreSQL. Nada importante dentro do container, pois o
  backup diário cobre apenas o banco.

## Frontend e contrato

- **Módulos ES:** módulos com responsabilidade única. Nada de arquivo gigante.
- **Separação de camadas:** separe rigorosamente acesso à API, gerência de estado e manipulação do
  DOM.
- **Tratamento de estado:** trate sempre carregando, erro e vazio nas chamadas à API.
- **Documentação da API:** OpenAPI (springdoc).
- **Tipagem:** os tipos TypeScript são gerados a partir do OpenAPI, não escritos à mão.

## Docker e segurança

- **Imagens eficientes:** multi-stage (build com Maven, runtime com JRE 21 enxuto).
- **Privilégios:** containers não rodam como root.
- **Isolamento de rede:** o PostgreSQL não expõe porta para fora da rede do Compose, exceto em
  desenvolvimento local.
- **Ambiente:** configuração via variáveis de ambiente. Nenhum valor hardcoded.
- **Segredos:** nenhum segredo (senha, token, chave) no código, no repositório, no Dockerfile ou em
  logs.
- **Validação:** valide toda entrada na fronteira (Bean Validation nos DTOs).
- **Autenticação:** endpoints são autenticados por padrão (Spring Security). Endpoint público precisa
  ser explicitamente marcado e justificado.
- **Vazamento de dados:** erros retornados ao cliente não vazam stack trace nem detalhes internos.
- **XSS e injeções:** no frontend, nunca insira dado do usuário via `innerHTML`. Use `textContent` ou
  crie elementos via DOM API.

## Testes

- **Cobertura:** todo código novo de regra de negócio vem com teste.
- **Testes unitários:** não sobem o contexto do Spring, para garantir velocidade.
- **Testes de integração:** com banco usam Testcontainers (PostgreSQL real, não H2).
- **Foco do teste:** teste comportamento, não implementação. O nome do teste descreve o cenário e o
  resultado esperado.

## Ao responder

Se o que for pedido conflitar com estes fundamentos, aponte o conflito. Se faltar informação para
decidir, pergunte em vez de presumir.
