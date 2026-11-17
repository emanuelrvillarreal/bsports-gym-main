# syntax=docker/dockerfile:1
# Sitio estático HTML servido por nginx. Contexto de build = raíz del repo.
# Por defecto copia public/; override con build-arg STATIC_DIR si hace falta.
ARG STATIC_DIR=public

FROM nginx:1.27-alpine

ARG STATIC_DIR
COPY ${STATIC_DIR}/ /usr/share/nginx/html/

COPY deploy/nginx-1.27/nginx.container.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
