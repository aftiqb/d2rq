package de.fuberlin.wiwiss.d2rq.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A map from table names to aliases. Can be applied to various objects and will 
 * replace all mentions of a table with its alias. For some kinds of objects, 
 * the inverse operation is available as well. 
 *
 * TODO: There is an assumption that one original table has at most one alias.
 * Otherwise, the applyTo() operations are indeterministic. This is troublesome.
 * All uses of this class that need applyTo() should probably be redesigned
 * to use something else than a "real" AliasMap.
 *  
 * TODO: AliasMap and ColumnRenamer are different concepts.
 * An AliasMap is a bunch of "table AS alias" declarations. A column renamer is
 * something that can be applied to a column name to yield a new one. It should
 * be possible to obtain a ColumnRenamer from a declared AliasMap, but they should
 * be separate classes. AliasMap might also be better called Aliases or AliasSet.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: AliasMap.java,v 1.9 2008/08/13 11:27:38 cyganiak Exp $
 */
public class AliasMap extends ColumnRenamer {
	public static final AliasMap NO_ALIASES = new AliasMap(Collections.EMPTY_SET);

	public static AliasMap create1(RelationName original, RelationName alias) {
		return new AliasMap(Collections.singletonList(new Alias(original, alias)));
	}
	
	public static class Alias {
		private RelationName original;
		private RelationName alias;
		public Alias(RelationName original, RelationName alias) {
			this.original = original;
			this.alias = alias;
		}
		public RelationName original() {
			return this.original;
		}
		public RelationName alias() {
			return this.alias;
		}
		public int hashCode() {
			return this.original.hashCode() ^ this.alias.hashCode();
		}
		public boolean equals(Object o) {
			if (!(o instanceof Alias)) return false;
			return this.alias.equals(((Alias) o).alias) && this.original.equals(((Alias) o).original);
		}
		public String toString() {
			return this.original + " AS " + this.alias;
		}
	}
	
	private Map byAlias = new HashMap();
	private Map byOriginal = new HashMap();
	
	public AliasMap(Collection aliases) {
		Iterator it = aliases.iterator();
		while (it.hasNext()) {
			Alias alias = (Alias) it.next();
			this.byAlias.put(alias.alias(), alias);
			this.byOriginal.put(alias.original(), alias);
		}
	}
	
	public boolean isAlias(RelationName name) {
		return this.byAlias.containsKey(name);
	}

	public boolean hasAlias(RelationName original) {
		return this.byOriginal.containsKey(original);
	}
	
	public RelationName applyTo(RelationName original) {
		if (!hasAlias(original)) {
			return original;
		}
		Alias alias = (Alias) this.byOriginal.get(original);
		return alias.alias();
	}
	
	public RelationName originalOf(RelationName name) {
		if (!isAlias(name)) {
			return name;
		}
		Alias alias = (Alias) this.byAlias.get(name);
		return alias.original();
	}
	
	public Attribute applyTo(Attribute attribute) {
		if (!hasAlias(attribute.relationName())) {
			return attribute;
		}
		return new Attribute(applyTo(attribute.relationName()), attribute.attributeName());
	}
	
	public Attribute originalOf(Attribute attribute) {
		if (!isAlias(attribute.relationName())) {
			return attribute;
		}
		return new Attribute(originalOf(attribute.relationName()), attribute.attributeName());
	}
	
	public Alias applyTo(Alias alias) {
		if (!hasAlias(alias.alias())) {
			return alias;
		}
		return new Alias(alias.original(), applyTo(alias.alias()));
	}
	
	public Alias originalOf(Alias alias) {
		if (!isAlias(alias.original())) {
			return alias;
		}
		// For some weird reason, substitute only the original side
		return new Alias(originalOf(alias.original()), alias.alias());
	}
	
	public Join applyTo(Join join) {
		if (!hasAlias(join.table1()) && !hasAlias(join.table2())) {
			return join;
		}
		return super.applyTo(join);
	}

	public AliasMap applyTo(AliasMap other) {
		if (this.byAlias.isEmpty()) {
			return other;
		}
		if (other.byAlias.isEmpty()) {
			return this;
		}
		Collection newAliases = new ArrayList();
		Iterator it = other.byAlias.values().iterator();
		while (it.hasNext()) {
			newAliases.add(applyTo((Alias) it.next()));
		}
		it = this.byAlias.values().iterator();
		while (it.hasNext()) {
			Alias alias = (Alias) it.next();
			if (other.isAlias(alias.original())) continue;
			newAliases.add(alias);
		}
		return new AliasMap(newAliases);
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof AliasMap)) {
			return false;
		}
		AliasMap otherAliasMap = (AliasMap) other;
		return this.byAlias.equals(otherAliasMap.byAlias);
	}
	
	public int hashCode() {
		return this.byAlias.hashCode();
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("AliasMap(");
		List tables = new ArrayList(this.byAlias.keySet());
		Collections.sort(tables);
		Iterator it = tables.iterator();
		while (it.hasNext()) {
			result.append(this.byAlias.get(it.next()));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		result.append(")");
		return result.toString();
	}
}
