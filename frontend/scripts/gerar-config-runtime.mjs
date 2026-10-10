// Gera runtime-config.js a partir da variável de ambiente API_BASE_URL (ex.: a URL do backend no
// Render, quando este script roda no build do Netlify). Sem a variável, gera o mesmo padrão vazio
// do arquivo commitado — frontend e backend na mesma origem.
import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const urlBase = process.env.API_BASE_URL ?? "";
const destino = fileURLToPath(new URL("../runtime-config.js", import.meta.url));
const conteudo = `window.__API_BASE_URL__ = ${JSON.stringify(urlBase)};\n`;

writeFileSync(destino, conteudo);
console.log(`runtime-config.js gerado com API_BASE_URL=${JSON.stringify(urlBase)}`);
