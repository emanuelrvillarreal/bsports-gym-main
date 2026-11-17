# syntax=docker/dockerfile:1
FROM python:3.11-slim

WORKDIR /app

# Crear usuario para no ejecutar como root, al igual que el template de spring
RUN groupadd --gid 10001 python \
    && useradd --uid 10001 --gid python --system --no-create-home --shell /usr/sbin/nologin python

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Ajustar permisos para logs o uploads si es necesario
RUN mkdir -p /app/logs /app/uploads && chown -R python:python /app

USER python:python

EXPOSE 8000

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${INTERNAL_PORT:-8000}"]
