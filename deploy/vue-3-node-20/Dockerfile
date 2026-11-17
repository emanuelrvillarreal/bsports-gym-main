# syntax=docker/dockerfile:1
ARG VITE_BUILD_MODE=production
ARG VITE_BASE_PATH=/

FROM node:20-alpine AS build
WORKDIR /app
ARG VITE_BUILD_MODE
ARG VITE_BASE_PATH

COPY package*.json ./
RUN npm ci

COPY . .
# Compilamos Vue usando Vite con soporte a base path y variables de entorno opcionales
RUN npm run build -- --mode ${VITE_BUILD_MODE} --base ${VITE_BASE_PATH}

FROM nginx:1.27-alpine
ARG VITE_BASE_PATH
ENV VITE_BASE_PATH=${VITE_BASE_PATH}
RUN mkdir -p /usr/share/nginx/html${VITE_BASE_PATH}
COPY --from=build /app/dist/ /usr/share/nginx/html${VITE_BASE_PATH}

COPY deploy/vue-3-node-20/nginx.container.conf /etc/nginx/templates/default.conf.template
EXPOSE 80
