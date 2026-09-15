// 浏览器验收用静态托管与真实 API 转发，不包含业务模拟。
import http from 'node:http';
import { readFile } from 'node:fs/promises';
import path from 'node:path';

const root = path.resolve('dist');
const target = new URL(process.env.CYYS_API_TARGET);
const types = { '.html': 'text/html; charset=utf-8', '.js': 'application/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon' };
http.createServer(async (req, res) => {
  if (req.url.startsWith('/api/')) {
    const proxy = http.request(new URL(req.url, target), { method: req.method, headers: { ...req.headers, host: target.host } }, upstream => {
      res.writeHead(upstream.statusCode, upstream.headers); upstream.pipe(res);
    });
    proxy.on('error', () => { res.writeHead(502, { 'content-type': 'application/json' }); res.end(JSON.stringify({ code: 502, msg: 'API connection failed' })); });
    req.pipe(proxy); return;
  }
  try {
    const relative = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
    const file = path.resolve(root, '.' + relative);
    if (file !== root && !file.startsWith(root + path.sep)) { res.writeHead(403); res.end(); return; }
    const requested = path.extname(file) ? file : path.join(root, 'index.html');
    const content = await readFile(requested);
    res.writeHead(200, { 'content-type': types[path.extname(requested)] || 'application/octet-stream', 'cache-control': 'no-store' });
    res.end(content);
  } catch { res.writeHead(404); res.end(); }
}).listen(18041, '127.0.0.1');
