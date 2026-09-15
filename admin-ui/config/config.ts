// Ant Design Pro 6.0.1 official preview baseline; CYYS runtime configuration.
import { join } from 'node:path';
import { defineConfig } from '@umijs/max';
import defaultSettings from './defaultSettings';
import routes from './routes';

export default defineConfig({
  alias: { '@root': join(__dirname, '..') },
  hash: true,
  esbuildMinifyIIFE: true,
  publicPath: '/',
  routes,
  ignoreMomentLocale: true,
  proxy: {
    '/api/': {
      target: process.env.CYYS_API_TARGET || 'http://127.0.0.1:8080',
      changeOrigin: true,
    },
  },
  fastRefresh: true,
  routePrefetch: {},
  manifest: {},
  model: {},
  initialState: {},
  title: 'CYYS',
  layout: { locale: false, ...defaultSettings },
  moment2dayjs: { preset: 'antd', plugins: ['duration', 'relativeTime'] },
  locale: { default: 'zh-CN', antd: true, baseNavigator: false },
  antd: {
    appConfig: {},
    configProvider: {
      variant: 'filled',
      theme: { token: { fontFamily: 'AlibabaSans, sans-serif' } },
    },
  },
  request: {},
  reactQuery: {},
  access: {},
  mock: false,
  headScripts: [{ src: '/scripts/loading.js', async: true }],
  chainWebpack(config) {
    config.module.rule('markdown').test(/\.md$/).type('asset/source');
  },
});
