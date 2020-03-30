package it.cnr.istc.stlab.pss.harvester;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Gli oggetti di questa classe mantengono le informazioni necessarie che
 * servono per raccogliere dati da uno sparql endpoint sorgente (chiamata
 * sorgente principale). Inoltre premete di integrare alle risorse raccolte
 * dalla sorgente principale altre informazioni provenienti da altre sorgenti
 * dati (chiamate sorgenti secondarie). Le indicazioni su quali dati prendere
 * dalle sorgenti secondarie e su come integrarli con quelli raccolti dalla
 * sorgente principale sono modellati attraverso LODPrimarySoruce
 * 
 * @author lgu
 *
 */
public class LODPrimarySource {

	private String sparqlEndpoint;
	private String graph, klass, filter, sparqlResourceSelector;
	private String[] additionalPredicates;
	private String[] patternsToExpand;
	private String[] queryToAdd;
	private int pagination = -1;
	private Set<String> entities;
	private Set<String> entitiesDownloaded;
	private List<LODSecondarySource> secondarySources = new ArrayList<>();
	private List<String> resourcesToGet = new ArrayList<>();
	private boolean useOnlyConstruct;

	public LODPrimarySource(String sparqlEndpoint, String graph, String sparqlResourceSelector, String klass,
			String[] additionalPredicates, String[] patternsToExpand, String[] queryToAdd, String filter,
			boolean useOnlyConstruct, int pagination) {
		super();
		this.sparqlEndpoint = sparqlEndpoint;
		this.graph = graph;
		this.klass = klass;
		this.filter = filter;
		this.additionalPredicates = additionalPredicates;
		this.patternsToExpand = patternsToExpand;
		this.queryToAdd = queryToAdd;
		this.sparqlResourceSelector = sparqlResourceSelector;
		this.useOnlyConstruct = useOnlyConstruct;
		this.pagination = pagination;
	}

	public LODPrimarySource(String sparqlEndpoint, String graph, String[] additionalPredicates,
			String[] patternsToExpand, String[] queryToAdd, String klass, String sparqlResourceSelector) {
		this(sparqlEndpoint, graph, sparqlResourceSelector, klass, additionalPredicates, patternsToExpand, queryToAdd,
				null, false, -1);
	}

	public Set<String> getEntities() {
		return entities;
	}

	public Set<String> getEntitiesDownloaded() {
		return entitiesDownloaded;
	}

	public String getSparqlEndpoint() {
		return sparqlEndpoint;
	}

	public String getGraph() {
		return graph;
	}

	public String getKlass() {
		return klass;
	}

	public String getFilter() {
		return filter == null ? "" : filter;
	}

	public String[] getAdditionalPredicates() {
		return additionalPredicates == null ? new String[] {} : additionalPredicates;
	}

	public String[] getResourcesToExpand() {
		return patternsToExpand == null ? new String[] {} : patternsToExpand;
	}

	public String[] getQueryToAdd() {
		return queryToAdd == null ? new String[] {} : queryToAdd;
	}

	public List<LODSecondarySource> getSecondarySources() {
		return secondarySources;
	}

	public LODPrimarySource setSecondarySources(List<LODSecondarySource> secondarySources) {
		this.secondarySources = secondarySources;
		return this;
	}

	public void addSecondarySources(LODSecondarySource secondarySource) {
		this.secondarySources.add(secondarySource);
	}

	public List<String> getResourcesToGet() {
		return resourcesToGet;
	}

	public LODPrimarySource setResourcesToGet(List<String> resourcesToGet) {
		this.resourcesToGet = resourcesToGet;
		return this;
	}

	public void addResourcesToGet(String resourceToGet) {
		this.resourcesToGet.add(resourceToGet);
	}

	public String getSparqlResourceSelector() {
		return sparqlResourceSelector;
	}

	public boolean isUseOnlyConstruct() {
		return useOnlyConstruct;
	}

	public void setUseOnlyConstruct(boolean useOnlyConstruct) {
		this.useOnlyConstruct = useOnlyConstruct;
	}

	public int getPagination() {
		return pagination;
	}

	public void setPagination(int pagination) {
		this.pagination = pagination;
	}

}
