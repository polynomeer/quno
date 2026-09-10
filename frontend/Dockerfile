# syntax=docker/dockerfile:1

# --- Dependencies stage -------------------------------------------------------
FROM node:22-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# --- Build stage -------------------------------------------------------
FROM node:22-slim AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# 빌드 타임에 API base URL을 주입한다 — Next.js는 NEXT_PUBLIC_* 값을 빌드 시점에 번들에 굽는다
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
ARG NEXT_PUBLIC_SITE_URL=http://localhost:3000
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL
RUN npm run build

# --- Runtime stage -------------------------------------------------------
FROM node:22-slim AS runtime
WORKDIR /app

RUN groupadd -r quno && useradd -r -g quno quno

# next.config.ts의 output: "standalone" 산출물만 복사한다 — node_modules 전체가 필요 없다
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
RUN chown -R quno:quno /app
USER quno

ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
EXPOSE 3000

ENTRYPOINT ["node", "server.js"]
