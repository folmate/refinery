/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { ChangeDesc, Transaction } from '@codemirror/state';
import { debounce } from 'lodash-es';
import { nanoid } from 'nanoid';

import type EditorStore from '../editor/EditorStore';
import CancelledError from '../utils/CancelledError';
import TimeoutError from '../utils/TimeoutError';
import getLogger from '../utils/getLogger';

import UpdateStateTracker from './UpdateStateTracker';
import type XtextWebSocketClient from './XtextWebSocketClient';
import {
  type ContentAssistEntry,
  ContentAssistResult,
  DocumentStateResult,
  FormattingResult,
  isConflictResult,
  OccurrencesResult,
  HoverResult,
} from './xtextServiceResults';

const UPDATE_TIMEOUT_MS = 500;

const log = getLogger('xtext.UpdateService');

export interface AbortSignal {
  aborted: boolean;
}

export type CancellableResult<T> =
  | { cancelled: false; data: T }
  | { cancelled: true };

export interface ContentAssistParams {
  caretOffset: number;

  proposalsLimit: number;
}

export default class UpdateService {
  readonly resourceName: string;

  private readonly tracker: UpdateStateTracker;

  private readonly idleUpdateLater = debounce(
    () => this.idleUpdate(),
    UPDATE_TIMEOUT_MS,
  );

  constructor(
    private readonly store: EditorStore,
    private readonly webSocketClient: XtextWebSocketClient,
    private readonly onUpdate: (text: string) => void,
  ) {
    this.resourceName = `${nanoid(7)}.problem`;
    this.tracker = new UpdateStateTracker(store);
  }

  get xtextStateId(): string | undefined {
    return this.tracker.xtextStateId;
  }

  computeChangesSinceLastUpdate(): ChangeDesc {
    return this.tracker.computeChangesSinceLastUpdate();
  }

  onReconnect(): void {
    this.tracker.invalidateStateId();
    this.updateFullTextOrThrow().catch((err: unknown) => {
      // Let E_TIMEOUT errors propagate, since if the first update times out,
      // we can't use the connection.
      if (err instanceof CancelledError) {
        // Content assist will perform a full-text update anyways.
        log.debug('Full text update cancelled');
        return;
      }
      log.error({ err }, 'Unexpected error during initial update');
    });
  }

  onTransaction(transaction: Transaction): void {
    if (this.tracker.onTransaction(transaction)) {
      this.idleUpdateLater();
    }
  }

  get opened(): boolean {
    return this.webSocketClient.opened;
  }

  private idleUpdate(): void {
    if (!this.webSocketClient.opened || !this.tracker.needsUpdate) {
      return;
    }
    if (!this.tracker.lockedForUpdate) {
      this.updateOrThrow().catch((err: unknown) => {
        if (err instanceof CancelledError || err instanceof TimeoutError) {
          log.debug('Idle update cancelled');
          return;
        }
        log.error({ err }, 'Unexpected error during scheduled update');
      });
    }
    this.idleUpdateLater();
  }

  /**
   * Makes sure that the document state on the server reflects recent
   * local changes.
   *
   * Performs either an update with delta text or a full text update if needed.
   * If there are not local dirty changes, the promise resolves immediately.
   *
   * @returns a promise resolving when the update is completed
   */
  private async updateOrThrow(): Promise<void> {
    if (!this.tracker.needsUpdate) {
      return;
    }
    this.onUpdate(this.store.state.sliceDoc());
    await this.tracker.runExclusive(() => this.updateExclusive());
  }

  private async updateExclusive(): Promise<void> {
    if (this.xtextStateId === undefined) {
      await this.updateFullTextExclusive();
    }
    const delta = this.tracker.prepareDeltaUpdateExclusive();
    if (delta === undefined) {
      return;
    }
    log.trace({ delta }, 'Editor delta');
    this.store.analysisStarted();
    const result = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'update',
      requiredStateId: this.xtextStateId,
      ...delta,
    });
    await this.handleDocumentUpdateResult(result);
  }

  private async handleDocumentUpdateResult(result: unknown): Promise<void> {
    const parsedDocumentStateResult = DocumentStateResult.safeParse(result);
    if (parsedDocumentStateResult.success) {
      this.tracker.setStateIdExclusive(parsedDocumentStateResult.data.stateId);
      return;
    }
    if (isConflictResult(result, 'invalidStateId')) {
      await this.updateFullTextExclusive();
    }
    throw parsedDocumentStateResult.error;
  }

  private updateFullTextOrThrow(): Promise<void> {
    return this.tracker.runExclusive(() => this.updateFullTextExclusive());
  }

  private async updateFullTextExclusive(): Promise<void> {
    log.debug('Performing full text update');
    this.tracker.prepareFullTextUpdateExclusive();
    this.store.analysisStarted();
    const result = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'update',
      fullText: this.store.state.doc.sliceString(0),
      concretize: this.store.concretize,
    });
    const { stateId } = DocumentStateResult.parse(result);
    this.tracker.setStateIdExclusive(stateId);
  }

  async updateConcretize(): Promise<void> {
    if (!this.opened) {
      return;
    }
    await this.tracker.runExclusive(() => this.updateConcretizeExclusive());
  }

  private async updateConcretizeExclusive(): Promise<void> {
    if (this.xtextStateId === undefined) {
      await this.updateFullTextExclusive();
      // We have already sent `this.store.concretize` with the update message.
      return;
    }
    const delta = this.tracker.prepareDeltaUpdateExclusive() ?? {
      // Always send some delta to trigger semantics re-computation.
      deltaOffset: 0,
      deltaReplaceLength: 0,
      deltaText: '',
    };
    const { concretize } = this.store;
    log.trace({ delta }, 'Editor delta with concretize: %s', concretize);
    this.store.analysisStarted();
    const result = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'update',
      requiredStateId: this.xtextStateId,
      ...delta,
      concretize,
    });
    await this.handleDocumentUpdateResult(result);
  }

  async fetchContentAssist(
    params: ContentAssistParams,
    signal: AbortSignal,
  ): Promise<ContentAssistEntry[]> {
    if (!this.tracker.hasPendingChanges && this.xtextStateId !== undefined) {
      return this.fetchContentAssistFetchOnly(params, this.xtextStateId);
    }
    try {
      return await this.tracker.runExclusive(
        () => this.fetchContentAssistExclusive(params, signal),
        true,
      );
    } catch (error) {
      if (
        (error instanceof CancelledError || error instanceof TimeoutError) &&
        signal.aborted
      ) {
        return [];
      }
      throw error;
    }
  }

  private async fetchContentAssistExclusive(
    params: ContentAssistParams,
    signal: AbortSignal,
  ): Promise<ContentAssistEntry[]> {
    if (this.xtextStateId === undefined) {
      await this.updateFullTextExclusive();
      if (this.xtextStateId === undefined) {
        throw new Error('failed to obtain Xtext state id');
      }
    }
    if (signal.aborted) {
      return [];
    }
    let entries: ContentAssistEntry[] | undefined;
    if (this.tracker.needsUpdate) {
      entries = await this.fetchContentAssistWithDeltaExclusive(
        params,
        this.xtextStateId,
      );
    }
    if (entries !== undefined) {
      return entries;
    }
    if (signal.aborted) {
      return [];
    }
    if (this.xtextStateId === undefined) {
      throw new Error('failed to obtain Xtext state id');
    }
    return this.fetchContentAssistFetchOnly(params, this.xtextStateId);
  }

  private async fetchContentAssistWithDeltaExclusive(
    params: ContentAssistParams,
    requiredStateId: string,
  ): Promise<ContentAssistEntry[] | undefined> {
    const delta = this.tracker.prepareDeltaUpdateExclusive();
    if (delta === undefined) {
      return undefined;
    }
    log.trace({ delta }, 'Editor delta for content assist');
    const fetchUpdateResult = await this.webSocketClient.send({
      ...params,
      resource: this.resourceName,
      serviceType: 'assist',
      requiredStateId,
      ...delta,
    });
    const parsedContentAssistResult =
      ContentAssistResult.safeParse(fetchUpdateResult);
    if (parsedContentAssistResult.success) {
      const {
        data: { stateId, entries },
      } = parsedContentAssistResult;
      this.tracker.setStateIdExclusive(stateId);
      return entries;
    }
    if (isConflictResult(fetchUpdateResult, 'invalidStateId')) {
      log.warn('Server state invalid during content assist');
      await this.updateFullTextExclusive();
      return undefined;
    }
    throw parsedContentAssistResult.error;
  }

  private async fetchContentAssistFetchOnly(
    params: ContentAssistParams,
    requiredStateId: string,
  ): Promise<ContentAssistEntry[]> {
    // Fallback to fetching without a delta update.
    const fetchOnlyResult = await this.webSocketClient.send({
      ...params,
      resource: this.resourceName,
      serviceType: 'assist',
      requiredStateId,
    });
    const { stateId, entries: fetchOnlyEntries } =
      ContentAssistResult.parse(fetchOnlyResult);
    if (stateId !== requiredStateId) {
      throw new Error(
        `Unexpected state id, expected: ${requiredStateId} got: ${stateId}`,
      );
    }
    return fetchOnlyEntries;
  }

  formatText(): Promise<void> {
    return this.tracker.runExclusive(() => this.formatTextExclusive());
  }

  private async formatTextExclusive(): Promise<void> {
    await this.updateExclusive();
    let { from, to } = this.store.state.selection.main;
    if (to <= from) {
      from = 0;
      to = this.store.state.doc.length;
    }
    log.debug('Formatting from %d to %d', from, to);
    const result = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'format',
      selectionStart: from,
      selectionEnd: to,
    });
    const { stateId, formattedText } = FormattingResult.parse(result);
    this.tracker.setStateIdExclusive(stateId, {
      from,
      to,
      insert: formattedText,
    });
  }

  async fetchOccurrences(
    getCaretOffset: () => CancellableResult<number>,
  ): Promise<CancellableResult<OccurrencesResult>> {
    try {
      await this.updateOrThrow();
    } catch (error) {
      if (error instanceof CancelledError || error instanceof TimeoutError) {
        return { cancelled: true };
      }
      throw error;
    }
    const expectedStateId = this.xtextStateId;
    if (expectedStateId === undefined || this.tracker.hasPendingChanges) {
      // Just give up if another update is in progress.
      return { cancelled: true };
    }
    const caretOffsetResult = getCaretOffset();
    if (caretOffsetResult.cancelled) {
      return { cancelled: true };
    }
    const data = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'occurrences',
      caretOffset: caretOffsetResult.data,
      expectedStateId,
    });
    if (
      isConflictResult(data) ||
      this.tracker.hasChangesSince(expectedStateId)
    ) {
      return { cancelled: true };
    }
    const parsedOccurrencesResult = OccurrencesResult.parse(data);
    if (parsedOccurrencesResult.stateId !== expectedStateId) {
      return { cancelled: true };
    }
    return { cancelled: false, data: parsedOccurrencesResult };
  }

  async fetchHoverTooltip(
    caretOffset: number,
  ): Promise<CancellableResult<HoverResult>> {
    try {
      await this.updateOrThrow();
    } catch (error) {
      if (error instanceof CancelledError || error instanceof TimeoutError) {
        return { cancelled: true };
      }
      throw error;
    }
    const expectedStateId = this.xtextStateId;
    if (expectedStateId === undefined || this.tracker.hasPendingChanges) {
      // Just give up if another update is in progress.
      return { cancelled: true };
    }
    const data = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'hover',
      caretOffset,
      expectedStateId,
    });
    if (
      isConflictResult(data) ||
      this.tracker.hasChangesSince(expectedStateId)
    ) {
      return { cancelled: true };
    }
    const parsedHoverResult = HoverResult.parse(data);
    if (parsedHoverResult.stateId !== expectedStateId) {
      return { cancelled: true };
    }
    return { cancelled: false, data: parsedHoverResult };
  }
}
