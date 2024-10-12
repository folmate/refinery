/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import { alpha } from '@mui/material/styles';
import {
  DataGrid,
  type GridRenderCellParams,
  type GridColDef,
} from '@mui/x-data-grid';
import { observer } from 'mobx-react-lite';
import { useMemo } from 'react';

import type GraphStore from '../graph/GraphStore';
import RelationName from '../graph/RelationName';

import TableToolbar from './TableToolbar';
import ValueRenderer from './ValueRenderer';

interface Row {
  nodes: string[];
  value: string;
}

declare module '@mui/x-data-grid' {
  // Declare our custom prop type for `TableToolbar`.
  interface ToolbarPropsOverrides {
    graph: GraphStore;
  }

  interface NoRowsOverlayPropsOverrides {
    graph: GraphStore;
  }

  interface NoResultsOverlayPropsOverrides {
    graph: GraphStore;
  }
}

const noSymbolMessage =
  'Please select a symbol from the list to view its interpretation';

function NoRowsOverlay({
  graph: { selectedSymbol },
}: {
  graph: GraphStore;
}): JSX.Element {
  return (
    <Stack
      height="100%"
      alignItems="center"
      justifyContent="center"
      textAlign="center"
      p={2}
    >
      {selectedSymbol === undefined ? (
        noSymbolMessage
      ) : (
        <span>
          Interpretation of <RelationName metadata={selectedSymbol} /> is empty
        </span>
      )}
    </Stack>
  );
}

function NoResultsOverlay({
  graph: { selectedSymbol },
}: {
  graph: GraphStore;
}): JSX.Element {
  return (
    <Stack height="100%" alignItems="center" justifyContent="center">
      {selectedSymbol === undefined ? (
        noSymbolMessage
      ) : (
        <span>
          No results in the interpretation of{' '}
          <RelationName metadata={selectedSymbol} />
        </span>
      )}
    </Stack>
  );
}

function TableArea({
  graph,
  touchesTop,
}: {
  graph: GraphStore;
  touchesTop: boolean;
}): JSX.Element {
  const {
    selectedSymbol,
    semantics: { nodes, partialInterpretation },
  } = graph;
  const symbolName = selectedSymbol?.name;
  const arity = selectedSymbol?.arity ?? 0;

  const columns = useMemo<GridColDef<Row>[]>(() => {
    const defs: GridColDef<Row>[] = [];
    for (let i = 0; i < arity; i += 1) {
      defs.push({
        field: `n${i}`,
        headerName: String(i + 1),
        valueGetter: (_, row) => row.nodes[i] ?? '',
        flex: 1,
      });
    }
    defs.push({
      field: 'value',
      headerName: 'Value',
      flex: 1,
      renderCell: ({ value }: GridRenderCellParams<Row, string>) => (
        <ValueRenderer value={value} />
      ),
    });
    return defs;
  }, [arity]);

  const rows = useMemo<Row[]>(() => {
    if (symbolName === undefined) {
      return [];
    }
    const interpretation = partialInterpretation[symbolName] ?? [];
    return interpretation.map((tuple) => {
      const nodeNames: string[] = [];
      for (let i = 0; i < arity; i += 1) {
        const index = tuple[i];
        if (typeof index === 'number') {
          const node = nodes[index];
          if (node !== undefined) {
            nodeNames.push(node.name);
          }
        }
      }
      return {
        nodes: nodeNames,
        value: String(tuple[arity]),
      };
    });
  }, [arity, nodes, partialInterpretation, symbolName]);

  return (
    <Box width="100%" height="100%">
      <DataGrid
        slots={{
          toolbar: TableToolbar,
          noRowsOverlay: NoRowsOverlay,
          noResultsOverlay: NoResultsOverlay,
        }}
        slotProps={{
          toolbar: {
            graph,
          },
          noRowsOverlay: {
            graph,
          },
          noResultsOverlay: {
            graph,
          },
        }}
        initialState={{ density: 'compact' }}
        rowSelection={false}
        columns={columns}
        rows={rows}
        getRowId={(row) => row.nodes.join(',')}
        sx={(theme) => ({
          border: 'none',
          '--DataGrid-rowBorderColor':
            theme.palette.mode === 'dark'
              ? alpha(theme.palette.text.primary, 0.24)
              : theme.palette.outer.border,
          '.MuiDataGrid-withBorderColor': {
            borderColor: theme.palette.outer.border,
          },
          '.MuiDataGrid-toolbarContainer': {
            background: touchesTop
              ? 'transparent'
              : theme.palette.outer.background,
            padding: theme.spacing(1),
            // Correct for the non-integer height of the text box to match up with the editor area toolbar.
            marginBottom: '-0.5px',
          },
          '.MuiDataGrid-columnHeaders': {
            '.MuiDataGrid-columnHeader, .MuiDataGrid-filler, .MuiDataGrid-scrollbarFiller':
              {
                background: theme.palette.outer.background,
                borderBottom: `1px solid ${theme.palette.outer.border}`,
                borderTop: touchesTop
                  ? `1px solid ${theme.palette.outer.border}`
                  : 'none',
              },
          },
          '.MuiDataGrid-row--firstVisible .MuiDataGrid-scrollbarFiller': {
            display: 'none',
          },
          '.MuiDataGrid-footerContainer': {
            backgroundColor: theme.palette.outer.background,
          },
          '.MuiDataGrid-columnSeparator': {
            color: theme.palette.text.disabled,
          },
        })}
      />
    </Box>
  );
}

export default observer(TableArea);
