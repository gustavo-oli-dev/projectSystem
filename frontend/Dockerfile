# syntax=docker/dockerfile:1

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json tsconfig.json ./
RUN npm install
COPY src ./src
RUN npm run build

FROM nginxinc/nginx-unprivileged:alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/index.html
COPY runtime-config.js /usr/share/nginx/html/runtime-config.js
COPY styles /usr/share/nginx/html/styles
COPY --from=build /app/dist /usr/share/nginx/html/dist
EXPOSE 8080
