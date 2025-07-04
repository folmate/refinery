/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CloudIcon from '@mui/icons-material/Cloud';
import CloudOffIcon from '@mui/icons-material/CloudOff';
import SyncIcon from '@mui/icons-material/Sync';
import SyncProblemIcon from '@mui/icons-material/SyncProblem';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import { keyframes, styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';

import type EditorStore from './EditorStore';

const rotateKeyframe = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(-360deg);
  }
`;

const AnimatedSyncIcon = styled(SyncIcon)`
  animation: ${rotateKeyframe} 1.4s linear infinite;
`;

export default observer(function ConnectButton({
  editorStore,
}: {
  editorStore: EditorStore | undefined;
}): React.ReactElement {
  if (
    editorStore !== undefined &&
    (editorStore.opening || editorStore.opened)
  ) {
    return (
      <Tooltip
        title={
          editorStore.opening
            ? 'Connecting (click to cancel)'
            : 'Connected (click to disconnect)'
        }
      >
        <span>
          <IconButton
            onClick={() => editorStore.disconnect()}
            aria-label="Disconnect"
            color="inherit"
          >
            {editorStore.opening ? (
              <AnimatedSyncIcon fontSize="small" />
            ) : (
              <CloudIcon fontSize="small" />
            )}
          </IconButton>
        </span>
      </Tooltip>
    );
  }

  let title: string;
  let disconnectedIcon: React.ReactElement;
  if (editorStore === undefined) {
    title = 'Connecting';
    disconnectedIcon = <SyncIcon fontSize="small" />;
  } else if (editorStore.connectionErrors.length > 0) {
    title = 'Connection error (click to retry)';
    disconnectedIcon = <SyncProblemIcon fontSize="small" />;
  } else {
    title = 'Disconnected (click to connect)';
    disconnectedIcon = <CloudOffIcon fontSize="small" />;
  }

  return (
    <Tooltip title={title}>
      <span>
        <IconButton
          disabled={editorStore === undefined}
          onClick={() => editorStore?.connect()}
          aria-label="Connect"
          color="inherit"
        >
          {disconnectedIcon}
        </IconButton>
      </span>
    </Tooltip>
  );
});
