/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable } from 'mobx';

import GraphStore, { type Visibility } from '../graph/GraphStore';
import type { SemanticsModelResult } from '../xtext/xtextServiceResults';

import type EditorStore from './EditorStore';

export default class GeneratedModelStore {
  title: string;

  message = 'Waiting for server';

  error = false;

  graph: GraphStore | undefined;

  savedVisibility: Map<string, Visibility>;

  constructor(
    private readonly randomSeed: number,
    private readonly editorStore: EditorStore,
  ) {
    const time = new Date().toLocaleTimeString(undefined, { hour12: false });
    this.title = `Generated at ${time} (${randomSeed})`;
    this.savedVisibility = new Map(editorStore.selectedGraph.visibility);
    makeAutoObservable<GeneratedModelStore, 'editorStore'>(this, {
      savedVisibility: false,
      editorStore: false,
    });
  }

  get running(): boolean {
    return !this.error && this.graph === undefined;
  }

  setMessage(message: string): void {
    if (this.running) {
      this.message = message;
    }
  }

  setError(message: string): void {
    if (this.running) {
      this.error = true;
      this.message = message;
    }
  }

  setSemantics(semantics: SemanticsModelResult, source?: string): void {
    if (this.running) {
      const name = `${this.editorStore.simpleNameOrFallback}_solution_${this.randomSeed}`;
      this.graph = new GraphStore(this.editorStore, name, this.savedVisibility);
      this.graph.setSemantics(semantics, source);
    }
  }
}
