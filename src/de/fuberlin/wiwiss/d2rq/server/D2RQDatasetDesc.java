package de.fuberlin.wiwiss.d2rq.server;

import java.util.Map;

import org.joseki.DatasetDesc;
import org.joseki.Request;
import org.joseki.Response;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A Joseki dataset description that returns a dataset
 * consisting only of the one ModelD2RQ passed in. We need
 * this because regular Joseki dataset descriptions are
 * initialized from a configuration Model, and we want
 * to initialize programmatically.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: D2RQDatasetDesc.java,v 1.6 2009/06/11 09:13:18 fatorange Exp $
 */
public class D2RQDatasetDesc extends DatasetDesc {
	private AutoReloadableDataset dataset;
	
	public D2RQDatasetDesc(AutoReloadableDataset dataset) {
		super(null);
		this.dataset = dataset;
	}

	@Override
	public Dataset acquireDataset(Request request, Response response) {
		dataset.checkMappingFileChanged();
		return this.dataset;
	}

	@Override
    public void returnDataset(Dataset ds) {
		// do nothing
	}

	public String toString() {
		return "D2RQDatasetDesc(" + this.dataset + ")";
	}
}
