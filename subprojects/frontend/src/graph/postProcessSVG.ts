/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { type BBox, parsePolygonBBox, parsePathBBox } from './parseBBox';

export const SVG_NS = 'http://www.w3.org/2000/svg';
export const XLINK_NS = 'http://www.w3.org/1999/xlink';

function parseCoordinate(element: SVGElement, attribute: string): number {
  return parseFloat(element.getAttribute(attribute)?.replace('px', '') ?? '0');
}

function modifyAttribute(
  element: SVGElement,
  attribute: string,
  change: number,
): number {
  const value = parseCoordinate(element, attribute);
  const newValue = value + change;
  element.setAttribute(attribute, String(newValue));
  return value;
}

function addShadow(
  node: SVGGElement,
  container: SVGRectElement,
  offset: number,
): void {
  const shadow = container.cloneNode() as SVGRectElement;
  // Leave space for 1pt stroke around the original container.
  const offsetWithStroke = offset - 0.5;
  modifyAttribute(shadow, 'x', offsetWithStroke);
  modifyAttribute(shadow, 'y', offsetWithStroke);
  modifyAttribute(shadow, 'width', 1);
  modifyAttribute(shadow, 'height', 1);
  modifyAttribute(shadow, 'rx', 0.5);
  modifyAttribute(shadow, 'ry', 0.5);
  shadow.setAttribute('class', 'node-shadow');
  shadow.id = `${node.id},shadow`;
  node.insertBefore(shadow, node.firstChild);
}

function clipCompartmentBackground(node: SVGGElement) {
  // Background rectangle of the node created by the `<table bgcolor="white">`
  // HTML element in dot. It was transformed into a rounded rect by `fixNodeBackground`.
  const container = node.querySelector<SVGRectElement>('rect[fill="white"]');
  // Background rectangle of the lower compartment created by the `<td bgcolor="green">`
  // HTML element in dot. It was transformed into a rounded rect by `fixNodeBackground`.
  // Since dot doesn't round the coners of `<td>` background,
  // we have to clip it ourselves.
  const compartment = node.querySelector<SVGRectElement>('rect[fill="green"]');
  // Make sure we provide traceability with IDs also for the border.
  const border = node.querySelector<SVGRectElement>('rect[stroke="black"]');
  if (container === null || compartment === null || border === null) {
    return;
  }
  const copyOfContainer = container.cloneNode() as SVGRectElement;
  const clipPath = document.createElementNS(SVG_NS, 'clipPath');
  const clipId = `${node.id},,clip`;
  clipPath.setAttribute('id', clipId);
  clipPath.appendChild(copyOfContainer);
  node.appendChild(clipPath);
  compartment.setAttribute('clip-path', `url(#${clipId})`);
  // Enlarge the compartment to completely cover the background.
  const y = modifyAttribute(compartment, 'y', -5);
  const x = modifyAttribute(compartment, 'x', -5);
  const width = modifyAttribute(compartment, 'width', 10);
  const isEmpty = node.classList.contains('node-empty');
  // Make sure that empty nodes are fully filled.
  const height = modifyAttribute(compartment, 'height', isEmpty ? 10 : 5);
  if (node.classList.contains('node-equalsSelf-UNKNOWN')) {
    addShadow(node, container, 6);
  }
  container.id = `${node.id},container`;
  compartment.id = `${node.id},compartment`;
  border.id = `${node.id},border`;
  const label = node.querySelector(':scope > text');
  if (label === null) {
    return;
  }
  label.setAttribute('dominant-baseline', 'middle');
  label.setAttribute('text-anchor', 'middle');
  label.setAttribute('x', String(x + width / 2));
  label.setAttribute('y', String(y + height / 2));
}

function createRect(
  { x, y, width, height }: BBox,
  original: SVGElement,
): SVGRectElement {
  const rect = document.createElementNS(SVG_NS, 'rect');
  rect.setAttribute('fill', original.getAttribute('fill') ?? '');
  rect.setAttribute('stroke', original.getAttribute('stroke') ?? '');
  rect.setAttribute('x', String(x));
  rect.setAttribute('y', String(y));
  rect.setAttribute('width', String(width));
  rect.setAttribute('height', String(height));
  return rect;
}

function optimizeNodeShapes(node: SVGGElement) {
  node.querySelectorAll('path').forEach((path) => {
    const bbox = parsePathBBox(path);
    const rect = createRect(bbox, path);
    rect.setAttribute('rx', '12');
    rect.setAttribute('ry', '12');
    path.parentNode?.replaceChild(rect, path);
  });
  node.querySelectorAll('polygon').forEach((polygon) => {
    const bbox = parsePolygonBBox(polygon);
    if (bbox.height === 0) {
      const polyline = document.createElementNS(SVG_NS, 'polyline');
      polyline.setAttribute('stroke', polygon.getAttribute('stroke') ?? '');
      polyline.setAttribute(
        'points',
        `${bbox.x},${bbox.y} ${bbox.x + bbox.width},${bbox.y}`,
      );
      polygon.parentNode?.replaceChild(polyline, polygon);
    } else {
      const rect = createRect(bbox, polygon);
      polygon.parentNode?.replaceChild(rect, polygon);
    }
  });
  clipCompartmentBackground(node);
}

function hrefToClass(node: SVGGElement) {
  node.querySelectorAll<SVGAElement>('a').forEach((a) => {
    if (a.parentNode === null) {
      return;
    }
    const href = a.getAttribute('href') ?? a.getAttributeNS(XLINK_NS, 'href');
    if (href === 'undefined' || !href?.startsWith('#')) {
      return;
    }
    while (a.lastChild !== null) {
      const child = a.lastChild;
      a.removeChild(child);
      if (child.nodeType === Node.ELEMENT_NODE) {
        const element = child as Element;
        element.classList.add('label', `label-${href.replace('#', '')}`);
        a.after(child);
      }
    }
    a.parentNode.removeChild(a);
  });
}

function replaceImages(node: SVGGElement) {
  node.querySelectorAll<SVGImageElement>('image').forEach((image) => {
    const href =
      image.getAttribute('href') ?? image.getAttributeNS(XLINK_NS, 'href');
    if (href === 'undefined' || !href?.startsWith('#')) {
      return;
    }
    const width = parseCoordinate(image, 'width');
    const height = parseCoordinate(image, 'height');
    const x = parseCoordinate(image, 'x');
    const y = parseCoordinate(image, 'y');
    const size = Math.min(width, height);
    const sizeString = String(size);
    const use = document.createElementNS(SVG_NS, 'use');
    const xOffset = x + (width - size) / 2;
    use.setAttribute('x', String(xOffset));
    const yOffset = y + (height - size) / 2;
    use.setAttribute('y', String(yOffset));
    use.setAttribute('width', sizeString);
    use.setAttribute('height', sizeString);
    const iconName = `icon-${href.replace('#', '')}`;
    use.setAttribute('href', `#refinery-${iconName}`);
    use.classList.add('icon', iconName);
    const sibling = image.nextElementSibling;
    // Since dot doesn't respect the `id` attribute on table cells with a single image,
    // compute the ID based on the ID of the next element (the label).
    if (
      sibling !== null &&
      sibling.tagName.toLowerCase() === 'g' &&
      sibling.id !== ''
    ) {
      use.id = `${sibling.id},icon`;
      sibling.querySelectorAll('text').forEach((textElement) => {
        // Fix rounded text placement by dot.
        textElement.setAttribute('dominant-baseline', 'middle');
        textElement.setAttribute('text-anchor', 'start');
        textElement.setAttribute('x', String(xOffset + size + 4));
        textElement.setAttribute('y', String(y + height / 2));
      });
    } else if (sibling !== null && sibling.tagName.toLowerCase() === 'text') {
      // Fix rounded text placement of error labels by dot.
      sibling.setAttribute('text-anchor', 'start');
      sibling.setAttribute('x', String(xOffset + size + 3));
    }
    image.parentNode?.replaceChild(use, image);
  });
}

function markerColorToClass(svg: SVGSVGElement) {
  svg.querySelectorAll('.node [stroke="black"]').forEach((node) => {
    node.removeAttribute('stroke');
    node.classList.add('node-outline');
  });
  svg.querySelectorAll('.node [fill="green"]').forEach((node) => {
    node.removeAttribute('fill');
    node.classList.add('node-header');
  });
  svg.querySelectorAll('.node [fill="white"]').forEach((node) => {
    node.removeAttribute('fill');
    node.classList.add('node-bg');
  });
  svg.querySelectorAll('.edge [stroke="black"]').forEach((node) => {
    node.removeAttribute('stroke');
    node.classList.add('edge-line');
  });
  svg.querySelectorAll('.edge [fill="black"]').forEach((node) => {
    node.removeAttribute('fill');
    node.classList.add('edge-arrow');
  });
  svg.querySelectorAll('[font-family]').forEach((node) => {
    node.removeAttribute('font-family');
  });
}

export default function postProcessSvg(svg: SVGSVGElement) {
  svg.querySelectorAll<SVGGElement>('g.node').forEach((node) => {
    optimizeNodeShapes(node);
    hrefToClass(node);
  });
  replaceImages(svg);
  // Increase padding to fit box shadows for multi-objects.
  const viewBox = [
    svg.viewBox.baseVal.x - 6,
    svg.viewBox.baseVal.y - 6,
    svg.viewBox.baseVal.width + 12,
    svg.viewBox.baseVal.height + 12,
  ];
  svg.setAttribute('viewBox', viewBox.join(' '));
  markerColorToClass(svg);
}
