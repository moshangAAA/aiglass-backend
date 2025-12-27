# AI Glass Backend - Deployment Guide

## 🎯 Prerequisites

### Required Software
- **Java**: OpenJDK 21
- **Docker**: 24.0+
- **Docker Compose**: 2.20+
- **Git**: 2.30+

### Server Requirements (Production)
- **CPU**: 2+ cores (4 recommended)
- **RAM**: 4GB minimum (8GB recommended)
- **Storage**: 20GB minimum
- **OS**: Ubuntu 20.04+ or Alibaba Cloud ECS

---

## 🚀 Quick Start (Development)

### 1. Clone Repository
```bash
git clone <your-repo-url>
cd aiglass-backend
```

### 2. Setup Environment
```bash
# Copy environment template
cp .env.example .env

# Edit configuration (important!)
nano .env
```

### 3. Start Services
```bash
# Start all services (MySQL, Redis, Backend)
docker-compose up -d

# Check logs
docker-compose logs -f backend

# Verify health
curl http://localhost:8080/actuator/health
```

### 4. Access Application
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html (dev only)
- **Health**: http://localhost:8080/actuator/health

---

## 🏭 Production Deployment

### Step 1: Server Setup (Alibaba Cloud ECS)

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify installations
docker --version
docker-compose --version
```

### Step 2: Clone and Configure

```bash
# Clone to production directory
sudo mkdir -p /opt/aiglass
sudo chown $USER:$USER /opt/aiglass
cd /opt/aiglass
git clone <your-repo-url> backend
cd backend
```

### Step 3: Configure Environment

```bash
# Copy production environment template
cp .env.prod.example .env

# Generate strong secrets
openssl rand -base64 32  # For JWT_SECRET
openssl rand -base64 24  # For MySQL passwords
openssl rand -base64 16  # For Redis password

# Edit production config
nano .env

# IMPORTANT: Set these values
# - SPRING_PROFILES_ACTIVE=prod
# - Strong passwords for MySQL and Redis
# - JWT_SECRET (use generated value above)
# - Aliyun SMS credentials (or use RAM role)
```

### Step 4: Configure Aliyun RAM Role (Recommended)

**Why use RAM Role?**
- ✅ No AccessKey/Secret in config files
- ✅ Automatic credential rotation
- ✅ Better security compliance

**Setup Steps:**
1. Go to Alibaba Cloud RAM Console
2. Create a new role for ECS service
3. Attach policy: `AliyunDysmsFullAccess`
4. Attach role to your ECS instance
5. Leave `ALIYUN_ACCESS_KEY_ID` and `ALIYUN_ACCESS_KEY_SECRET` empty in `.env`

### Step 5: Build and Deploy

```bash
# Set production profile
export SPRING_PROFILES_ACTIVE=prod

# Build application
docker-compose build

# Start services
docker-compose up -d

# Monitor startup
docker-compose logs -f backend

# Wait for "Started AiglassBackendApplication" message
```

### Step 6: Verify Deployment

```bash
# Check all containers are healthy
docker-compose ps

# Expected output:
# aiglass-mysql    Up (healthy)
# aiglass-redis    Up (healthy)
# aiglass-backend  Up (healthy)

# Test health endpoint
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}

# Test authentication endpoint
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "phoneNumber": "+8613800138000",
    "password": "Test123!@#"
  }'
```

---

## 🌐 Nginx Reverse Proxy (Optional but Recommended)

### Install Nginx
```bash
sudo apt install nginx -y
```

### Configure Nginx
```bash
sudo nano /etc/nginx/sites-available/aiglass
```

Add this configuration:
```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    # SSL certificates (use Certbot/Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # API endpoints
    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # WebSocket support
    location /ws {
        proxy_pass http://localhost:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

Enable and start:
```bash
sudo ln -s /etc/nginx/sites-available/aiglass /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

---

## 📊 Monitoring & Maintenance

### View Logs
```bash
# All services
docker-compose logs -f

# Backend only
docker-compose logs -f backend

# Last 100 lines
docker-compose logs --tail=100 backend

# Application logs (inside container)
docker exec aiglass-backend tail -f /var/log/aiglass/application.log
```

### Check Resource Usage
```bash
# Container stats
docker stats

# Disk usage
docker system df
```

### Database Backup
```bash
# Create backup
docker exec aiglass-mysql mysqldump \
  -u root -p${MYSQL_ROOT_PASSWORD} \
  aiglass_db > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore backup
docker exec -i aiglass-mysql mysql \
  -u root -p${MYSQL_ROOT_PASSWORD} \
  aiglass_db < backup_20241227_120000.sql
```

### Redis Backup
```bash
# Trigger Redis save
docker exec aiglass-redis redis-cli --rdb /data/dump.rdb

# Copy backup
docker cp aiglass-redis:/data/dump.rdb ./redis_backup.rdb
```

---

## 🔄 Update & Rollback

### Update Application
```bash
# Pull latest code
cd /opt/aiglass/backend
git pull origin main

# Rebuild and restart
docker-compose down
docker-compose build --no-cache backend
docker-compose up -d

# Verify
docker-compose logs -f backend
```

### Rollback to Previous Version
```bash
# Check git history
git log --oneline

# Rollback to specific commit
git checkout <commit-hash>

# Rebuild
docker-compose build --no-cache backend
docker-compose up -d
```

---

## 🆘 Troubleshooting

### Application Won't Start

**Check logs:**
```bash
docker-compose logs backend
```

**Common issues:**

1. **Database connection failed**
   - Solution: Check MySQL is healthy: `docker-compose ps`
   - Solution: Verify credentials in `.env`

2. **Redis connection failed**
   - Solution: Check REDIS_PASSWORD matches in `.env`

3. **Port already in use**
   - Solution: `sudo lsof -i :8080` and kill the process
   - Or change SERVER_PORT in `.env`

4. **Flyway migration failed**
   - Solution: Check database user has correct permissions
   - Solution: `GRANT ALL PRIVILEGES ON aiglass_db.* TO 'aiglass_user'@'%';`

### Out of Memory

Edit Dockerfile and increase heap:
```dockerfile
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC"
```

Rebuild:
```bash
docker-compose build --no-cache backend
docker-compose up -d backend
```

### Slow Performance

**Check database connections:**
```bash
docker exec aiglass-mysql mysql -u root -p -e "SHOW PROCESSLIST;"
```

**Check Redis memory:**
```bash
docker exec aiglass-redis redis-cli INFO memory
```

**Increase pool sizes in application-prod.yml:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 15
```

---

## 🔒 Security Checklist

- [ ] Change all default passwords
- [ ] Use strong JWT_SECRET (32+ characters)
- [ ] Enable HTTPS with valid SSL certificate
- [ ] Configure firewall (allow only 80, 443, 22)
- [ ] Use RAM Role instead of AccessKey for Aliyun
- [ ] Disable Swagger in production (SWAGGER_ENABLED=false)
- [ ] Set proper file permissions: `chmod 600 .env`
- [ ] Enable database SSL in production
- [ ] Configure Redis requirepass
- [ ] Regular security updates: `sudo apt update && sudo apt upgrade`

---

## 📞 Support

- **Team**: Lento Team
- **Documentation**: `/docs`
- **API Docs**: `/docs/api/HARDWARE_INTEGRATION.md`
- **Database Schema**: `/docs/database/`

---

## 📈 Performance Tuning

### JVM Tuning (Dockerfile)
```dockerfile
# For 4GB RAM server
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# For 8GB RAM server
ENV JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### MySQL Tuning
Add to `docker-compose.yml`:
```yaml
mysql:
  command: 
    - --max_connections=200
    - --innodb_buffer_pool_size=1G
    - --query_cache_size=0
```

### Redis Tuning
```yaml
redis:
  command: >
    redis-server
    --maxmemory 512mb
    --maxmemory-policy allkeys-lru
    --tcp-backlog 511
```

---

## 🎓 Next Steps

1. Setup monitoring with Prometheus + Grafana
2. Configure log aggregation (ELK Stack)
3. Implement CI/CD pipeline
4. Add automated backups with cron
5. Setup alerting for downtime
6. Load testing with JMeter

