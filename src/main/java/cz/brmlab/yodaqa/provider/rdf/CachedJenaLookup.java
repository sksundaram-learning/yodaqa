package cz.brmlab.yodaqa.provider.rdf;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;

/** This is an abstract base class for accessing various RDF resources,
 * typically DBpedia.  We leverage Apache Jena for the backend, plus
 * employ a cache store of already obtained query responses, since by
 * nature we will have a massive repeat rate of our queries.  Our
 * interface is optimized for direct triplet resolution rather than
 * complex queries.
 *
 * TODO: Actually do cache the response. :-) */

public abstract class CachedJenaLookup {
	/* XXX: In theory, we should have an extra class in the hierachy
	 * with these DBpedia specific defaults */
	/* Replace this value with http://dbpedia.org/sparql to use the
	 * public DBpedia SPARQL endpoint. */
	protected String service = "http://pasky.or.cz:8890/sparql";
	protected String prefixes =
		"PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
		"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
		"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
		"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
		"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
		"PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
		"PREFIX : <http://dbpedia.org/resource/>\n" +
		"PREFIX dbpedia2: <http://dbpedia.org/property/>\n" +
		"PREFIX dbpedia: <http://dbpedia.org/>\n" +
		"PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
		"PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
		"";

	/** Initialize a CachedJenaLookup object.  Points at
	 * the DBpedia SPARQL endpoint. */
	public CachedJenaLookup() {
	}

	/** Initialize a CachedJenaLookup object.  Communicates
	 * with the given SPARQL @service endpoint. */
	public CachedJenaLookup(String service_) {
		service = service_;
	}

	/** Issue a select statement, returning list of resource
	 * values.
	 *
	 * Example: rawQuery("?lab rdfs:label \"Achilles\"@en", "lab"); */
	public List<Literal[]> rawQuery(String selectWhere, String resources[]) {
		String queryExpr = prefixes + "SELECT ?"
			+ StringUtils.join(resources, " ?")
			+ " WHERE { " + selectWhere + " }";
		QueryExecution qe = QueryExecutionFactory.sparqlService(service, queryExpr);

		ResultSet rs;
		while (true) {
			try {
				rs = qe.execSelect();
				break; // Success!
			} catch (QueryExceptionHTTP e) {
				e.printStackTrace();
				System.err.println("*** DBpedia SPARQL Query (temporarily?) failed, retrying in a moment...");
				try {
					TimeUnit.SECONDS.sleep(10);
				} catch (InterruptedException e2) { // oof...
					e2.printStackTrace();
				}
			}
		}

		List<Literal[]> results = new LinkedList<Literal[]>();
		while (rs.hasNext()) {
			QuerySolution s = rs.nextSolution();
			Literal[] result = new Literal[resources.length];
			for (int i = 0; i < resources.length; i++)
				result[i] = s.getLiteral("?" + resources[i]);
			results.add(result);
		}

		return results;
	}
}
