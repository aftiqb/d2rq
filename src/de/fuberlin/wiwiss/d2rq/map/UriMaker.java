/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/
package de.fuberlin.wiwiss.d2rq.map;

import java.util.*;

import com.hp.hpl.jena.graph.*;

import de.fuberlin.wiwiss.d2rq.TablePrefixer;
import de.fuberlin.wiwiss.d2rq.rdql.NodeConstraint;

/**
 * UriMakers transform attribute values from a result set into URIrefs.
 * They are used within TripleMakers.
 *
 * <p>History:<br>
 * 06-21-2004: Initial version of this class.<br>
 * 08-03-2004: added couldFit, getColumns, getColumnValues
 * 
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
class UriMaker implements NodeMaker, Prefixable {
	private ValueSource valueSource;
	private String id;

	public Object clone() throws CloneNotSupportedException {return super.clone();}
	public void prefixTables(TablePrefixer prefixer) {
		valueSource=prefixer.prefixValueSource(valueSource);
	}
	
	public void matchConstraint(NodeConstraint c) {
        c.matchNodeType(NodeConstraint.UriNodeType);
        c.matchValueSource(valueSource);
	}       

	
	public UriMaker(String id, ValueSource valueSource) {
		this.valueSource = valueSource;
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#couldFit(com.hp.hpl.jena.graph.Node)
	 */
	public boolean couldFit(Node node) {
		if (Node.ANY.equals(node)) {
			return true;
		}
		return node.isURI() && this.valueSource.couldFit(node.getURI());
	}

	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#getColumns()
	 */
	public Set getColumns() {
		return this.valueSource.getColumns();
	}
	/* (non-Javadoc)
	 * @see de.fuberlin.wiwiss.d2rq.NodeMaker#getColumnValues(com.hp.hpl.jena.graph.Node)
	 */
	public Map getColumnValues(Node node) {
		return this.valueSource.getColumnValues(node.getURI());
	}

	public Node getNode(String[] row, Map columnNameNumberMap) {
		String value = this.valueSource.getValue(row, columnNameNumberMap);
		if (value == null) {
			return null;
		}
		return Node.createURI(value);
	}
	
	public String toString() {
		return "URIMaker@" + this.id;
	}
}