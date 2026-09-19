# 珞珈声场小程序

该目录保存 uni-app + Vue 3 + TypeScript 源码，不包含 `node_modules`、`unpackage`、微信开发者工具个人配置或任何真实密钥。

## 本地开发

1. 复制 `.env.example` 为 `.env.local`，按本地端口修改 API、WebSocket 和媒体地址。
2. 在 `manifest.json` 的 `mp-weixin.appid` 中填入自己的微信小程序 AppID。
3. 使用 HBuilderX 打开本目录，运行到微信开发者工具。

AppSecret 只能由后端通过环境变量读取，不得写入小程序源码。根目录 [`.env.example`](../.env.example) 列出了后端所需变量。
