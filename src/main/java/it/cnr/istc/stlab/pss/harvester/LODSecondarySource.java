package it.cnr.istc.stlab.pss.harvester;

/**
 * 
 * Questa classe modella i dati che devono essere catturati da una sorgente secondaria e come questi dati devono essere integrati con una sorgente primaria.
 * 
 * @author lgu
 *
 */
public class LODSecondarySource {

	// Endpoint della sorgente secondaria
	private String sparqlEndpoint;

	// Questo campo memorizza i triple pattern che associano una risorsa sull'endpoint primario (indicata come ?resource)
	// la risorsa sull'endpoint primario che viene riferita dall'esterno (indicata come ?refResource)
	private String patternToIdentifyURIPointedToExternalSource;

	// Le query che verranno eseguite sullo sparql endpoint secondario che restituiranno le triple che sullo sparql endpoint
	// secondario sono relative a ?refResource
	private String[] queries;

	public LODSecondarySource(String sparqlEndpoint, String[] queries) {
		super();
		this.sparqlEndpoint = sparqlEndpoint;
		this.queries = queries;
	}

	public String getSparqlEndpoint() {
		return sparqlEndpoint;
	}

	public void setSparqlEndpoint(String sparqlEndpoint) {
		this.sparqlEndpoint = sparqlEndpoint;
	}

	public String getPatternToIdentifyURIPointedToExternalSource() {
		return patternToIdentifyURIPointedToExternalSource;
	}

	public void setPatternToIdentifyURIPointedToExternalSource(String patternToIdentifyURIPointedToExternalSource) {
		this.patternToIdentifyURIPointedToExternalSource = patternToIdentifyURIPointedToExternalSource;
	}

	public String[] getQueries() {
		return queries;
	}

	public void setQueries(String[] queries) {
		this.queries = queries;
	}

}
