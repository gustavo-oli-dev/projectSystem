// Endereço do backend quando ele não está na mesma origem do frontend. Vazio = caminho relativo
// "/api" (caso do Docker local, onde o nginx deste frontend faz proxy de /api para o backend).
//
// No Netlify este arquivo é gerado de novo no build (ver netlify.toml e
// scripts/gerar-config-runtime.mjs), a partir da variável de ambiente API_BASE_URL — esta versão
// commitada é só o padrão para quem roda localmente ou via Docker Compose.
window.__API_BASE_URL__ = "";
