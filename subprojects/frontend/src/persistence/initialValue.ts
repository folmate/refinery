/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export default `% Metamodel
abstract class Object.
class Component extends Object.
class No extends Object.

pred concrete(node) <-> ::builtin::exists(node), ::builtin::equals(node,node).

discrete 0.9::Operational pred isOperational(Component c)<-> concrete(c), Component(c).

/*// Should produce error as pred cannot? contain to event.
pred compound1(Component c) <->
    isOperational(c) not { Operational }.
*/

event pred compound(Component c)<->
    isOperational(c) not { Operational }.

pred bar(Component obj)<->Component(obj), concrete(obj).

Component(a).
Component(b).
Component(c).
%Component(d).

No(g).

!::builtin::exists(f).

% Scope

scope node = 3..5.
`;
