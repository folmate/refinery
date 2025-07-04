/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { PluginOption, ServerOptions } from 'vite';

import backendConfigVitePlugin, {
  type BackendConfig,
} from './backendConfigVitePlugin';

export const API_ENDPOINT = 'api';
export const XTEXT_ENDPOINT = 'xtext-service';
export const CHAT_ENDPOINT = 'chat';

export interface DevModeOptions {
  mode: string;
  isDevelopment: boolean;
  devModePlugins: PluginOption[];
  serverOptions: ServerOptions;
}

interface ListenOptions {
  host: string;
  port: number;
  secure: boolean;
}

function detectListenOptions(
  name: string,
  fallbackHost: string,
  fallbackPort: number,
): ListenOptions {
  const host = process.env[`REFINERY_${name}_HOST`] ?? fallbackHost;
  const rawPort = process.env[`REFINERY_${name}_PORT`];
  const port = rawPort === undefined ? fallbackPort : parseInt(rawPort, 10);
  const secure = port === 443;
  return { host, port, secure };
}

function listenURL(
  { host, port, secure }: ListenOptions,
  protocol = 'http',
): string {
  return `${secure ? `${protocol}s` : protocol}://${host}:${port}`;
}

export default function detectDevModeOptions(): DevModeOptions {
  const mode = process.env['MODE'] ?? 'development';
  const isDevelopment = mode === 'development';

  if (!isDevelopment) {
    return {
      mode,
      isDevelopment,
      devModePlugins: [],
      serverOptions: {},
    };
  }

  const listen = detectListenOptions('LISTEN', 'localhost', 1313);
  // Make sure we always use IPv4 to connect to the backend,
  // because it doesn't listen on IPv6.
  const api = detectListenOptions('API', '127.0.0.1', 1312);
  const apiURL = listenURL(api);
  const chat = detectListenOptions('CHAT', '127.0.0.1', 1314);
  const chatURL = listenURL(chat);
  const publicAddress = detectListenOptions('PUBLIC', listen.host, listen.port);
  const publicURL = listenURL(publicAddress);

  if (listen.secure) {
    // Since nodejs 20, we'd need to pass in HTTPS options manually.
    throw new Error(`Preview on secure port ${listen.port} is not supported`);
  }

  const backendConfig: BackendConfig = {
    apiBase: `${publicURL}/${API_ENDPOINT}/v1`,
    webSocketURL: `${listenURL(publicAddress, 'ws')}/${XTEXT_ENDPOINT}`,
    chatURL: `${publicURL}/${CHAT_ENDPOINT}/v1`,
  };

  return {
    mode,
    isDevelopment,
    devModePlugins: [backendConfigVitePlugin(backendConfig)],
    serverOptions: {
      host: listen.host,
      port: listen.port,
      strictPort: true,
      headers: {
        // Enable strict origin isolation, see e.g.,
        // https://github.com/vitejs/vite/issues/3909#issuecomment-1065893956
        'Cross-Origin-Opener-Policy': 'same-origin',
        'Cross-Origin-Embedder-Policy': 'require-corp',
        'Cross-Origin-Resource-Policy': 'cross-origin',
      },
      proxy: {
        [`/${API_ENDPOINT}`]: {
          target: apiURL,
          secure: api.secure,
        },
        [`/${XTEXT_ENDPOINT}`]: {
          target: apiURL,
          ws: true,
          secure: api.secure,
        },
        [`/${CHAT_ENDPOINT}`]: {
          target: chatURL,
          secure: chat.secure,
        },
      },
      hmr: {
        host: publicAddress.host,
        clientPort: publicAddress.port,
        path: '/vite',
      },
    },
  };
}
