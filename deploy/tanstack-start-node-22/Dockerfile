# syntax=docker/dockerfile:1
# Node.js 22 + TanStack Start SSR (build + runtime). Contexto de build = raíz del repo.
ARG VITE_BASE_PATH=/
ARG VITE_BUILD_MODE=production

FROM node:22-bookworm AS build
WORKDIR /app
ARG VITE_BASE_PATH
ARG VITE_BUILD_MODE

ENV VITE_BASE_PATH=${VITE_BASE_PATH}

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN if [ "$VITE_BUILD_MODE" = "production" ]; then npm run build; else npm run build -- --mode "$VITE_BUILD_MODE"; fi

FROM node:22-bookworm-slim AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOST=0.0.0.0

COPY package.json package-lock.json ./
RUN npm ci --omit=dev

COPY --from=build /app/dist ./dist
COPY server.mjs ./

USER node

EXPOSE 3000

CMD ["node", "server.mjs"]
