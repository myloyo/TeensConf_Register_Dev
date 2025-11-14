# Stage 1: Build the React app
FROM node:18-alpine as build

WORKDIR /app

# Copy package files
COPY package*.json ./
# Install all dependencies (including devDependencies) so the build step
# can run (devDependencies are required for building CRA / webpack).
RUN npm ci

# Copy source code
COPY . .

# Build the app
RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:alpine

# Copy built app
COPY --from=build /app/build /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]