```
docker rm -f blacklist-server && \
docker run -d  --restart unless-stopped \
  --add-host blacklist.telega:35.228.245.151 \
  -e TG_BASE_URL="http://blacklist.telega/bot" \
  -e TG_BOT_TOKEN="832581890:AAH21UTXTdYWavGAPBgY5isJqDgxO0CGZ7g" \
  -e PG_USER=blacklist \
  -e PG_HOST=postgres \
  -e PG_PASSWORD=7758b15055b27403a1d0714ad9430794 \
  --link blacklist-postgres:postgres \
  --cpus 0.5 \
  --memory 512m \
  --name blacklist-server \
  dbarashev/org.spbelect.blacklist:20200322
```
```
docker rm -f blacklist-letsencrypt &&  \
docker run -d --restart unless-stopped  \
    --name blacklist-letsencrypt \
    --cap-add=NET_ADMIN \
    -e PUID=1001 \
    -e PGID=1001 \
    -e TZ=Europe/Moscow \
    -e URL=spbelect.org \
    -e SUBDOMAINS=blacklist, \
    -e ONLY_SUBDOMAINS=true \
    -e VALIDATION=http \
    -e EMAIL='info+blacklist@spbelect.org' \
    -p 4433:443 \
    -p 8080:80 \
    -v $(pwd)/letsencrypt-config:/config \
    --link blacklist-server:blacklist_backend \
    --cpus 0.1 --memory 64m linuxserver/letsencrypt
```
