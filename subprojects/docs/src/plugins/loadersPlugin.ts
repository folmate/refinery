/*
 * Copyright (c) 2016, Jeremy Stucki
 * Copyright (c) Facebook, Inc. and its affiliates.
 * Copyright (c) 2024-2025 The Refinery Authors
 *
 * SPDX-License-Identifier: BSD-3-Clause AND MIT AND EPL-2.0
 */

import type { Plugin } from '@docusaurus/types';
import { getFileLoaderUtils } from '@docusaurus/utils';
// @ts-expect-error No typings available for `responsive-loader`.
import sharp from 'responsive-loader/sharp';

export default function loadersPlugin(): Plugin {
  return {
    name: 'refinery-loaders-plugin',
    configureWebpack(config, isServer) {
      let rules = [...(config.module?.rules ?? [])];
      const utils = getFileLoaderUtils(isServer);
      /* eslint-disable-next-line @typescript-eslint/no-base-to-string --
        The `@docusarus/plugin-svgr` plugin also uses a conversion to `String` here.
      */
      const svgTest = String(utils.rules.svgs().test);
      const existingSvgRule = rules.find(
        (rule) =>
          typeof rule === 'object' &&
          rule !== null &&
          'test' in rule &&
          /* eslint-disable-next-line @typescript-eslint/no-base-to-string --
            The `@docusarus/plugin-svgr` plugin also uses a conversion to `String` here.
          */
          String(rule.test) === svgTest,
      );
      // Configuration based on
      // https://github.com/facebook/docusaurus/blob/d9d7e855c248e86b05ee86821622a631b802b56e/packages/docusaurus-plugin-svgr/src/svgrLoader.ts
      // but we re-create it here to disable options that inerfere with styling figures and add an exception for ?url.
      const newSvgRule = {
        test: /\.svg$/i,
        oneOf: [
          {
            issuer: {
              // We don't want to use SVGR loader for non-React source code
              // ie we don't want to use SVGR for CSS files...
              and: [/\.(?:tsx?|jsx?|mdx?)$/i],
            },
            resourceQuery: {
              // Skip SVGR when importing SVG files with ?url.
              not: /[?&]url$/i,
            },
            use: [
              {
                loader: '@svgr/webpack',
                options: {
                  prettier: false,
                  svgo: true,
                  svgoConfig: {
                    plugins: [
                      {
                        name: 'preset-default',
                        params: {
                          overrides: {
                            removeTitle: false,
                            removeViewBox: false,
                            // Disable SVGO, because it interferes styling figures exported from Refinery with CSS.
                            inlineStyles: false,
                            cleanupIds: false,
                          },
                        },
                      },
                    ],
                  },
                  titleProp: true,
                },
              },
            ],
          },
          {
            use: [utils.loaders.url({ folder: 'images' })],
          },
        ],
      };
      if (existingSvgRule) {
        rules[rules.indexOf(existingSvgRule)] = newSvgRule;
      } else {
        rules = [newSvgRule, ...rules];
      }
      return {
        mergeStrategy: {
          'module.rules': 'replace',
        },
        module: {
          rules: [
            // Configuration based on
            // https://github.com/dazuaz/responsive-loader/blob/ef2c806fcd36f06f6be8a0b97e09f40c3d86d3ac/README.md
            {
              test: /\.(png|jpe?g)$/,
              resourceQuery: /[?&]rl$/,
              use: [
                {
                  loader: 'responsive-loader',
                  options: {
                    /* eslint-disable-next-line @typescript-eslint/no-unsafe-assignment --
                     * No typings available for `responsive-loader`.
                     */
                    adapter: sharp,
                    format: 'webp',
                    // See
                    // https://github.com/facebook/docusaurus/blob/c745021b01a8b88d34e1d772278d7171ad8acdf5/packages/docusaurus-plugin-ideal-image/src/index.ts#L62-L66
                    emitFile: !isServer,
                    name: 'assets/images/[name].[hash:hex:7].[width].[ext]',
                  },
                },
              ],
            },
            ...rules,
          ],
        },
      };
    },
  };
}
