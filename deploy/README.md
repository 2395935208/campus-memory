# Alibaba Cloud ECS deployment

## Verified live deployment

- Public app: [http://47.239.40.162](http://47.239.40.162)
- Health check: [http://47.239.40.162/api/health](http://47.239.40.162/api/health)
- Region: China (Hong Kong)
- Runtime: Ubuntu 22.04, Docker, and Docker Compose
- Persistence: named Docker volume `campusmemory-data`
- Administration: Alibaba Cloud Session Manager; public SSH is disabled

## Fast path

1. Create an Alibaba Cloud ECS instance running Ubuntu 22.04 or later.
2. Allow inbound TCP `80` in its security group. Use Alibaba Cloud Session Manager for administration. If SSH is temporarily required, restrict it to the operator's current IP and remove the rule after deployment.
3. Paste `aliyun-ecs-user-data.sh` into **User Data** when creating the instance, then supply `DASHSCOPE_API_KEY` securely after startup.
4. Point a DNS name to the ECS public IP, or use `http://<ECS_PUBLIC_IP>` for judging.
5. Verify `http://<ECS_PUBLIC_IP>/api/health` returns `{"status":"UP","qwen":"configured"}`.

## Manual deployment

```bash
git clone https://github.com/2395935208/campus-memory.git /opt/campus-memory
cd /opt/campus-memory
read -rsp 'DASHSCOPE_API_KEY: ' DASHSCOPE_API_KEY && echo
printf 'DASHSCOPE_API_KEY=%s\nHOST_PORT=80\n' "$DASHSCOPE_API_KEY" > .env
unset DASHSCOPE_API_KEY
chmod 600 .env
docker compose up -d --build
curl http://localhost/api/health
```

`HOST_PORT=80` publishes the app on standard HTTP port 80. The `campusmemory-data` Docker volume preserves H2 data across container restarts.

## Submission evidence checklist

- Screenshot the Alibaba Cloud ECS instance details with the public IP visible.
- Record the [public app](http://47.239.40.162) and [health endpoint](http://47.239.40.162/api/health) in the demo video.
- Link the Devpost deployment-proof field to [`deploy/aliyun-ecs-user-data.sh`](https://github.com/2395935208/campus-memory/blob/main/deploy/aliyun-ecs-user-data.sh).
- Keep the app publicly reachable, free of charge, through the judging period.
