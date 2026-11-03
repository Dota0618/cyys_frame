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
  layout: { locale: false, ...defaultSettings },
  moment2dayjs: { preset: 'antd', plugins: ['duration', 'relativeTime'] },
  locale: { default: 'zh-CN', antd: true, baseNavigator: false },
  antd: {
    appConfig: {},
    configProvider: {
      variant: 'filled',
      theme: {
        token: { fontFamily: 'AlibabaSans, sans-serif', fontSize: 14, controlHeight: 32 },
        components: {
          Menu: { itemHeight: 40, itemMarginBlock: 4, itemMarginInline: 4,
            itemBorderRadius: 6, subMenuItemBorderRadius: 6, iconSize: 16 },
          Card: { bodyPadding: 12, bodyPaddingSM: 12, headerPadding: 12, headerPaddingSM: 12,
            headerHeight: 44, headerHeightSM: 44, headerFontSize: 14, headerFontSizeSM: 14 },
          Table: { cellPaddingBlock: 12, cellPaddingBlockMD: 10, cellPaddingBlockSM: 10,
            cellPaddingInline: 12, cellPaddingInlineMD: 12, cellPaddingInlineSM: 12 },
          Statistic: { contentFontSize: 24, titleFontSize: 14 },
          Descriptions: { itemPaddingBottom: 12 },
        },
      },
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
