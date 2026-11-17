# syntax=docker/dockerfile:1
# Node.js 22 + Angular SSR (build + runtime). Contexto de build = raíz del repo.
ARG NG_BUILD_CONFIGURATION=production
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

FROM node:22-bookworm-slim AS runtime
WORKDIR /app
ARG NG_APP_BASE_HREF=/

ENV NODE_ENV=production
ENV PORT=4000
ENV HOST=0.0.0.0
ENV APP_BASE_HREF=${NG_APP_BASE_HREF}

COPY --from=build /src/dist /tmp/dist
RUN server="$(find /tmp/dist -path '*/server/server.mjs' 2>/dev/null | head -1)" \
    && test -n "$server" \
    || (echo "ERROR: no se encontró server/server.mjs en dist (¿SSR habilitado con ng add @angular/ssr?)" >&2; exit 1) \
    && app_dir="$(dirname "$(dirname "$server")")" \
    && cp -a "${app_dir}/." /app/ \
    && rm -rf /tmp/dist \
    && chown -R node:node /app

USER node

EXPOSE 4000

CMD ["node", "server/server.mjs"]
