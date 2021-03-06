/*******************************************************************************
 * Copyright (C) 2003-2012 Bob Foster. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 * 
 *    Bob Foster, initial API and implementation.
 *******************************************************************************/
  
package org.genantics.peggen;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Minimal AST Node.
 * @author Bob Foster
 */
public class Node {

  // These are the output fields
  
	public String name;
	public int offset;
	public int length;
	public Node parent;
	public Node child;
	public Node next;
  
  // The remaining fields are only used during parsing
  
	// child and next are not used during the parse
	// these can be fixed up in one pass through the
	// array on exit
	public Node last;
	// nextout is only used durine the parse
	// (could be in separate int array)
	public int nextout;
	// success, remove and collect are only used during the parse
	// (they could be in separate boolean arrays)
	// success tells whether the rule matched
	// remove tells whether the node is excess baggage
	// collect says make the node the parent of preceeding peer nodes
	public boolean success;
	public boolean remove;
	public boolean collect;
  
	public Node(String name, Node parent, int offset) {
		this.name = name;
		this.parent = parent;
		this.offset = offset;
	}

	/**
	 * Pack a generated parse tree (array of Node) by
	 * removing all nodes for which remove is true
	 * and initializing parent, child and next links.
	 * The returned array is sized to fit the number
	 * of remaining nodes.
	 * @param array Generated Node array.
	 * @param top Highest entry used plus one,
	 * e.g., 0 for an empty array.
	 */
	public static Node[] pack(Node[] array, int top) {
		collect(array, top);
		top = remove(array, top);
		link(array, top);
		if (top < array.length) {
			Node[] tmp = new Node[top];
			System.arraycopy(array,0,tmp,0,top);
			array = tmp;
		}
		return array;
	}
	
	private static void collect(Node[] array, int top) {
		link(array, top);
		LinkedList stack = new LinkedList();
		stack.addLast(array[0]);
		while (!stack.isEmpty()) {
			Node parent = (Node) stack.removeFirst();
			Node child = parent.child;
			int length = 0;
			for (; child != null; child = child.next) {
				if (child.collect) {
					child.offset = parent.child.offset;
					child.length = length;
					for (Node collected = parent.child; collected != child; collected = collected.next) {
						collected.parent = child;
					}
					parent.child = child;
				}
				else {
					length += child.length;
					stack.addLast(child);
				}
			}
		}
	}

	private static int remove(Node[] array, int top) {
		HashMap replace = new HashMap();
		int to = 0;
		for (int i = 0; i < top; i++) {
			Node node = array[i];
			if (node.remove) {
				Node parent = node.parent;
				Node next;
				while ((next = (Node) replace.get(parent)) != null)
					parent = next;
				replace.put(node, parent);
			}
			else {
				Node parent = (Node) replace.get(node.parent);
				if (parent != null)
					node.parent = parent;
				array[to++] = node;
			}
		}
		return to;
	}

	private static void link(Node[] array, int top) {
		HashMap memo = new HashMap();
		Node toplevel = null;
		for (int i = 0; i < top; i++) {
			Node node = array[i];
			node.next = null;
			if (node.parent == null) {
				if (toplevel != null)
					toplevel.next = node;
				toplevel = node;
			}
			else {
				Node last = (Node) memo.get(node.parent);
				if (last == null)
					node.parent.child = node;
				else
					last.next = node;
				memo.put(node.parent, node);
			}
		}
	}

}
