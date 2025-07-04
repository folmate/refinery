/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Transaction } from '@codemirror/state';
import { Refinery, RefineryError } from '@tools.refinery/client';
import { nanoid } from 'nanoid';

import type EditorStore from '../editor/EditorStore';
import getLogger from '../utils/getLogger';

import type { BackendConfigWithDefaults } from './fetchBackendConfig';

const log = getLogger('xtext.ModelGenerationService');

const INITIAL_RANDOM_SEED = 1;

export default class ModelGenerationService {
  private readonly client: Refinery | undefined;
  private nextRandomSeed = INITIAL_RANDOM_SEED;
  private abortController: AbortController | undefined;

  constructor(
    private readonly store: EditorStore,
    { apiBase }: BackendConfigWithDefaults,
  ) {
    this.client = new Refinery({ baseURL: apiBase });
  }

  onTransaction(transaction: Transaction): void {
    if (transaction.docChanged) {
      this.resetRandomSeed();
    }
  }

  onDisconnect(): void {
    this.store.modelGenerationCancelled();
    this.resetRandomSeed();
  }

  start(randomSeed?: number): void {
    const { client } = this;
    if (client === undefined) {
      log.error('Refinery client not initialized');
      return;
    }
    this.abortController = new AbortController();
    const signal = this.abortController.signal;
    const randomSeedOrNext = randomSeed ?? this.nextRandomSeed;
    this.nextRandomSeed = randomSeedOrNext + 1;
    const uuid = nanoid();
    this.store.addGeneratedModel(uuid, randomSeedOrNext);
    (async () => {
      const { json, source } = await client.generate(
        {
          input: {
            source: this.store.state.sliceDoc(),
          },
          format: {
            json: {
              enabled: true,
              nonExistingObjects: 'discard',
              shadowPredicates: 'discard',
            },
            source: { enabled: true },
          },
          randomSeed: randomSeedOrNext,
        },
        {
          signal,
          onStatus: (status) => {
            this.store.setGeneratedModelMessage(uuid, status.message);
          },
        },
      );
      if (json === undefined) {
        log.error('Generated model is missing');
        this.store.setGeneratedModelError(uuid, 'Internal error');
        return;
      }
      this.store.setGeneratedModelSemantics(uuid, json, source);
    })().catch((err: unknown) => {
      if (err instanceof RefineryError.Cancelled) {
        log.warn('Model generation cancelled');
        if (this.abortController !== undefined) {
          // We only need to signal cancellation if it is not initiated by the user.
          // User cancellation is handled by the `cancel` method.
          this.store.modelGenerationCancelled();
        }
        return;
      }
      log.error({ err }, 'Error while generating model');
      this.store.setGeneratedModelError(
        uuid,
        err instanceof Error ? err.message : 'Unknown error',
      );
    });
  }

  cancel(): void {
    if (this.abortController !== undefined) {
      const { abortController } = this;
      this.abortController = undefined;
      abortController.abort();
    }
    this.store.modelGenerationCancelled();
  }

  private resetRandomSeed() {
    this.nextRandomSeed = INITIAL_RANDOM_SEED;
  }
}
