# ==============================================================================
# Production Dockerfile for Frontend (Multi-stage Node.js build -> Nginx Alpine)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build React/Vite Application
# ------------------------------------------------------------------------------
FROM node:22-alpine AS builder

WORKDIR /app

# Install dependencies using cache
COPY apps/frontend/package*.json ./
RUN npm ci || npm install

# Build arguments for client-side environment variables
ARG VITE_API_BASE_URL
ARG VITE_WS_BASE_URL

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_WS_BASE_URL=${VITE_WS_BASE_URL}

# Copy frontend source files
COPY apps/frontend/ ./

# Build production bundle
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Serve static assets with Nginx Alpine
# ------------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runner

# Remove default Nginx welcome page
RUN rm -rf /usr/share/nginx/html/*

# Copy built artifacts from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx SPA configuration
COPY docker/prod/nginx.conf /etc/nginx/conf.d/default.conf

# Expose HTTP port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
