# Resource Pack Troubleshooting

Common issues when setting up the resource pack system.

## Pack Not Downloading

**Symptoms:** Players don't receive the resource pack prompt on join.

**Solutions:**

1. **Check if the server is running:**
   ```
   /curios rp info
   ```
   If the pack is not available, the HTTP server may have failed to start.

2. **Verify the port is open:**
   - Ensure the port in `config.yml` is not blocked by a firewall
   - Test from outside: `curl http://your-ip:8080/pack.zip`

3. **Check `host-ip`:**
   - Set it to your server's **public** IP, not `localhost` (unless testing locally)
   - The IP must be reachable by players

4. **Port conflicts:**
   - Make sure no other service is using the same port
   - Try a different port (e.g., 8081, 9090)

## Textures Not Showing

**Symptoms:** Items show the base material texture instead of custom textures.

**Solutions:**

1. **Rebuild the pack:**
   ```
   /curios rp rebuild
   ```

2. **Re-download on client:**
   - Disconnect and reconnect to trigger a re-download
   - Or press F3+T to reload textures

3. **Check namespace:**
   - Ensure model paths match between config and resource pack files
   - Item model: `curiospaper:ring_slot` → `assets/curiospaper/models/item/ring_slot.json`

4. **Verify JSON syntax:**
   - Use a JSON validator to check model files
   - Common issues: trailing commas, missing brackets

## Namespace Conflicts

**Symptoms:** Warning messages about conflicts when the pack is built.

**Solutions:**

1. **View conflicts:**
   ```
   /curios rp conflicts
   ```

2. **Resolve conflicts:**
   - Rename conflicting files to use unique paths
   - Or enable `allow-namespace-conflicts: true` (not recommended)

3. **Check registered plugins:**
   - Multiple plugins may register assets with the same namespace

## Server Won't Start (Port in Use)

**Symptoms:** Error in console about port already in use.

**Solutions:**

1. Change the port in `config.yml`:
   ```yaml
   resource-pack:
     port: 8081
   ```

2. Find what's using the port:
   ```bash
   # Linux
   lsof -i :8080
   # Windows
   netstat -ano | findstr :8080
   ```

## Pack Too Large

**Symptoms:** Players experience long download times or timeouts.

**Solutions:**

1. Optimize texture sizes (use 16×16 where possible)
2. Use PNG compression tools (e.g., `pngquant`, `optipng`)
3. Remove unused textures from registered asset folders
