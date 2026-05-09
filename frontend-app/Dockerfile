# STAGE 1: Compilazione React
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# STAGE 2: Nginx Web Server
FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# Sostituisce la configurazione di default di Nginx con la nostra
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Il front espone la sua 80
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]