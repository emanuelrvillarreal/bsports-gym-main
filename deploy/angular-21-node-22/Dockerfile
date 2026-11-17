# syntax=docker/dockerfile:1
# Node.js 22 + pnpm + Angular CLI. Contexto de build = raíz del repo.
ARG NG_BUILD_CONFIGURATION=production
# Debe coincidir con el prefijo del reverse proxy (deploy.yml `path`), p. ej. /convenios/
ARG NG_APP_BASE_HREF=/

FROM node:22-bookworm AS build
WORKDIR /src
ARG NG_BUILD_CONFIGURATION
ARG NG_APP_BASE_HREF

RUN corepack enable \
    && pnpm config set store-dir /pnpm/store

COPY package.json ./
COPY pnpm-lock.yaml* package-lock.json* yarn.lock* .yarnrc.yml* ./
RUN --mount=type=bind,source=.,target=/context,readonly \
    --mount=type=cache,target=/pnpm/store \
    if [ -f /context/pnpm-workspace.yaml ]; then cp /context/pnpm-workspace.yaml ./; fi \
    && if [ ! -f pnpm-lock.yaml ]; then echo "ERROR: los stacks Angular requieren pnpm. Agrega pnpm-lock.yaml en la raiz del proyecto y elimina yarn.lock/package-lock.json." >&2; exit 1; fi \
    && if [ -f yarn.lock ] || [ -f package-lock.json ]; then echo "ERROR: los stacks Angular solo soportan pnpm. Elimina yarn.lock/package-lock.json y genera pnpm-lock.yaml." >&2; exit 1; fi \
    && node -e "const fs=require('fs'); const pkg=JSON.parse(fs.readFileSync('package.json','utf8')); const manager=pkg.packageManager; if (manager && !manager.startsWith('pnpm@')) { console.error('ERROR: package.json declara packageManager=' + manager + '; los stacks Angular solo soportan pnpm.'); process.exit(1); }" \
    && pnpm install --frozen-lockfile --prefer-offline

COPY . .
RUN pnpm exec ng build --configuration="${NG_BUILD_CONFIGURATION}" --base-href="${NG_APP_BASE_HREF}"

FROM nginx:1.27-alpine
COPY --from=build /src/dist /tmp/dist
RUN d="$(find /tmp/dist -maxdepth 4 -type d -name browser 2>/dev/null | head -1)" \
    && test -n "$d" \
    && cp -a "$d"/. /usr/share/nginx/html/ \
    && rm -rf /tmp/dist

COPY deploy/angular-21-node-22/nginx.container.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
