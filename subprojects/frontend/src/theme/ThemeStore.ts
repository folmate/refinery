/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable } from 'mobx';

export enum ThemePreference {
  System,
  PreferLight,
  PreferDark,
}

export type SelectedPane = 'code' | 'graph' | 'table' | 'chat';

export default class ThemeStore {
  preference = ThemePreference.System;

  systemDarkMode: boolean;

  showCode = true;

  showGraph = true;

  showTable = false;

  showChat = false;

  constructor() {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    this.systemDarkMode = mediaQuery.matches;
    mediaQuery.addEventListener('change', (event) => {
      this.systemDarkMode = event.matches;
    });
    makeAutoObservable(this, {
      isShowing: false,
    });
  }

  get darkMode(): boolean {
    switch (this.preference) {
      case ThemePreference.PreferLight:
        return false;
      case ThemePreference.PreferDark:
        return true;
      default:
        return this.systemDarkMode;
    }
  }

  toggleDarkMode(): void {
    if (this.darkMode) {
      this.preference = this.systemDarkMode
        ? ThemePreference.PreferLight
        : ThemePreference.System;
    } else {
      this.preference = this.systemDarkMode
        ? ThemePreference.System
        : ThemePreference.PreferDark;
    }
  }

  togglePane(pane: SelectedPane) {
    switch (pane) {
      case 'code':
        this.toggleCode();
        break;
      case 'graph':
        this.toggleGraph();
        break;
      case 'table':
        this.toggleTable();
        break;
      case 'chat':
        this.toggleChat();
        break;
      default:
        throw new Error(`Unknown pane: ${String(pane)}`);
    }
  }

  isShowing(pane: SelectedPane): boolean {
    switch (pane) {
      case 'code':
        return this.showCode;
      case 'graph':
        return this.showGraph;
      case 'table':
        return this.showTable;
      case 'chat':
        return this.showChat;
      default:
        throw new Error(`Unknown pane: ${String(pane)}`);
    }
  }

  toggleCode(): void {
    if (!this.showGraph && !this.showTable) {
      return;
    }
    this.showCode = !this.showCode;
  }

  toggleGraph(): void {
    if (!this.showCode && !this.showTable) {
      return;
    }
    this.showGraph = !this.showGraph;
  }

  toggleTable(): void {
    if (!this.showCode && !this.showGraph) {
      return;
    }
    this.showTable = !this.showTable;
  }

  toggleChat(): void {
    this.showChat = !this.showChat;
  }

  get selectedPane(): SelectedPane {
    if (this.showCode) {
      return 'code';
    }
    if (this.showGraph) {
      return 'graph';
    }
    if (this.showTable) {
      return 'table';
    }
    return 'code';
  }

  setSelectedPane(pane: SelectedPane, keepCode = true): void {
    if (pane === 'chat') {
      this.showChat = true;
      return;
    }
    this.showCode = pane === 'code' || (keepCode && this.showCode);
    this.showGraph = pane === 'graph';
    this.showTable = pane === 'table';
  }
}
