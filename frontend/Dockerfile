FROM node:22.23.1-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.27-alpine AS runtime
# This is a deployment default, not a build argument. Override at docker run/Compose.
# Missing backend must not prevent Nginx from starting.
ENV SIXPAY_BACKEND_UPSTREAM=backend:8080
# The official Nginx entrypoint expands exported environment variables at
# container startup; it writes default.conf from the template.
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/dist/frontend/browser/ /usr/share/nginx/html/
EXPOSE 80
