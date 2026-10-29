# ==============================================================================
# Development Dockerfile for Vite Frontend (HMR Mode)
# ==============================================================================
FROM node:22-alpine

WORKDIR /app

# Pre-install dependencies
COPY apps/frontend/package*.json ./
RUN npm install

# Copy application source
COPY apps/frontend/ ./

EXPOSE 5173

ENV HOST=0.0.0.0
ENV PORT=5173

# Start Vite dev server bound to 0.0.0.0
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]
