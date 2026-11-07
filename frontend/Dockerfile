FROM node:22.23.1-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
ARG BUILD_CONFIGURATION=integration
RUN npm run build -- --configuration ${BUILD_CONFIGURATION}

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/frontend/browser/ /usr/share/nginx/html/
EXPOSE 80
