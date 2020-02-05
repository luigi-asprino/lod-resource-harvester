package it.cnr.istc.stlab.pss.harvester;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import it.cnr.istc.stlab.lgu.commons.files.FileUtils;

public class Harvester {

	private static Logger logger = LogManager.getLogger(Harvester.class);
	private static final int NUMBER_OF_ATTEMPTS = 3;
	// private static final long TIMEOUT_QUERY = 60000;
	public static final long TIMEOUT_1_MINUTES = 2;
	public static final long TIMEOUT_2_MINUTES = 2;
	private static final long SLEEP_RETRY = 60000;

	public static void harvest(List<DownloadTask> l)
			throws IOException, JSchException, SftpException, InterruptedException {

		boolean excludeSSH = HarvesterConfiguration.getPSSConfiguration().excludeSSH();

		for (DownloadTask t : l) {

			LocalDestination d = t.getLocalDestination();
			File directory = new File(d.getLocalPath());
			directory.mkdirs();

			// get list of resources already downloaded
			Set<String> alreadyDownloadedResources = getAlreadyDownloadedResources(d);

			// get list of resources to download
			Set<String> resourcesToDownload = new HashSet<>();
			if (t.getSource().getKlass() == null && t.getSource().getSparqlResourceSelector() == null) {
				throw new RuntimeException("Non è stato fornito nessun criterio per la selezione delle entità!");
			}

			if (t.getSource().getKlass() != null) {
				resourcesToDownload = getResourcesOfAType(t.getSource());
			} else if (t.getSource().getSparqlResourceSelector() != null) {
				resourcesToDownload = getResourcesUsingSparql(t.getSource());
			} else {
				throw new RuntimeException(
						"Non è stata fornita nè una classe nè una query poter selezionare le risorse da scaricare!");
			}

			Set<String> resourcesToDownloadInThisTask = Sets.difference(resourcesToDownload,
					alreadyDownloadedResources);

			logger.info(String.format("Number of resource of type %s %s", t.getSource().getKlass(),
					resourcesToDownload.size()));
			logger.info(String.format("Number of resource already downloaded %s", alreadyDownloadedResources.size()));
			logger.info(String.format("Number of resources to download %s", resourcesToDownloadInThisTask.size()));
			logger.info(String.format("Limit %s", t.getLimit()));

			List<String> resourcesOrdered = new ArrayList<>(resourcesToDownloadInThisTask);
			if (t.getLimit() > 0)
				resourcesOrdered = resourcesOrdered.subList(0, t.getLimit());
			resourcesOrdered.addAll(t.getSource().getResourcesToGet());

			FileOutputStream f = new FileOutputStream(new File(d.getDownloadedFile()), true);
			FileOutputStream fun = new FileOutputStream(new File(d.getDownloadedFile()), true);
			ChannelSftp channel = null;
			if (t.getRemoteDestination() != null && !excludeSSH) {
				channel = getChannel(t.getRemoteDestination().getUser(), t.getRemoteDestination().getPassword(),
						t.getRemoteDestination().getHost());
			}
			int c = 0;
			for (String resourceToGet : resourcesOrdered) {

				logger.info(String.format("Getting resource %s %s", c++ + "/" + resourcesToDownloadInThisTask.size(),
						resourceToGet));

				if (resourceToGet == null) {
					logger.info(String.format("Resource {}", resourceToGet));
					continue;
				}

				// boolean resourceDownloaded = false;
				boolean errorsInDownloading = false;

				for (int i = 0; i < NUMBER_OF_ATTEMPTS; i++) {
					try {
						String filePath = storeResources(resourceToGet, t.getSource(), directory.getAbsolutePath());
						if (t.getRemoteDestination() != null && !excludeSSH) {
							// send to remote destination
							logger.info(String.format("Uploading the resource on the remote destination {}",
									resourceToGet));
							sendFile(channel, filePath,
									t.getRemoteDestination().getFolderPath() + "/" + FilenameUtils.getName(filePath));
							logger.info(String.format("Resource uploaded on the remote destination {}", resourceToGet));
						}
						// resourceDownloaded = true;
						break;
					} catch (SftpException e) {
						logger.warn("Connection Broken, new connection attempt in 60 seconds");
						Thread.sleep(SLEEP_RETRY);
						// new attempt to send the file
						channel = getChannel(t.getRemoteDestination().getUser(), t.getRemoteDestination().getPassword(),
								t.getRemoteDestination().getHost());
					} catch (RiotException e) {
						// some resources may have illegal characters and are discarded
						logger.warn("Failed");
						logger.error(e.getMessage());
						logger.warn(String.format("Discarding {}", resourceToGet));
						errorsInDownloading = true;
						e.printStackTrace();
					} catch (Exception e) {
						logger.warn("Failed " + e.getLocalizedMessage());
						e.printStackTrace();
						if (i < NUMBER_OF_ATTEMPTS) {
							logger.info(String.format("New attempt in {} seconds", (SLEEP_RETRY / 1000)));
							Thread.sleep(SLEEP_RETRY);
						} else {
							errorsInDownloading = true;
							e.printStackTrace();
						}
					}
				}

				if (errorsInDownloading) {
					logger.error(String.format("Impossible to download {}", resourceToGet));
					fun.write((resourceToGet + "\n").getBytes());
				}

				// mark the resource as already downloaded and uploaded
				if (!errorsInDownloading && !excludeSSH) {
					logger.info("Mark the resource as uploaded");
					f.write((resourceToGet + "\n").getBytes());
				}
			}

			f.close();
			fun.close();

			if (channel != null && channel.isConnected())
				channel.disconnect();
			if (session != null && session.isConnected())
				session.disconnect();

			logger.info("Task completed");
		}

	}

	private static Set<String> getAlreadyDownloadedResources(LocalDestination destination) {
		File f = new File(destination.getDownloadedFile());
		if (f.exists()) {
			return new HashSet<>(FileUtils.readFileToListString(f.getAbsolutePath()));
		} else {
			return new HashSet<>();
		}
	}

	private static Session session;

	private static ChannelSftp getChannel(String user, String password, String host) throws JSchException {
		int port = 22;
		if (password != null) {
			JSch jsch = new JSch();
			session = jsch.getSession(user, host, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
			logger.info(String.format("Establishing SSH Connection host {}", host));
			session.connect();
			logger.info("Connection established.");
			logger.info("Creating SFTP Channel.");
			ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			logger.info("SFTP Channel created.");

			return sftpChannel;
		} else {
			JSch jsch = new JSch();
			Session session = null;
			jsch.addIdentity(HarvesterConfiguration.getPSSConfiguration().getPrivatePathKey());
			session = jsch.getSession(user, host, port);
			session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			logger.info(String.format("Establishing SSH Connection host {}", host));
			session.connect();
			logger.info("Connection established.");
			logger.info("Creating SFTP Channel.");
			ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			logger.info("SFTP Channel created.");
			return sftpChannel;
		}

	}

	private static void sendFile(ChannelSftp sftpChannel, String localPath, String remotePath)
			throws SftpException, IOException {
		sftpChannel.put(localPath, remotePath);
		// remove local file
		Files.delete(new File(localPath).toPath());
	}

	private static String storeResources(String resourceToGet, LODPrimarySource source, String folderPath)
			throws FileNotFoundException {

		Model m = ModelFactory.createDefaultModel();
		logger.trace("Getting data related to resource");

		if (!source.isUseOnlyConstruct())
			m.add(getDataRelatedToRelatedToResource(resourceToGet, source, false));

		// adding additional patterns
		if (!source.isUseOnlyConstruct()) {
			m.add(m.createResource(resourceToGet), RDF.type, m.createResource("https://w3id.org/pss/CrawledResource"));
		}

		// adding additional predicates
		Set<String> additionalResources = new HashSet<>();
		if (!source.isUseOnlyConstruct()) {
			logger.trace("get additional resources using predicates");
			for (String p : source.getAdditionalPredicates()) {
				m.listObjectsOfProperty(m.createProperty(p)).forEachRemaining(o -> {
					if (o.isURIResource()) {
						additionalResources.add(o.asResource().getURI());
					}
				});
			}
		}

		// adding additional patterns
		if (!source.isUseOnlyConstruct()) {
			logger.trace("get additional resources using patterns");
			for (String pattern : source.getResourcesToExpand()) {
				String query = "SELECT DISTINCT ?resource { " + pattern + "}";
				// logger.trace(query);
				ParameterizedSparqlString pss = new ParameterizedSparqlString(query);
				pss.setIri("base", resourceToGet);
				// QueryExecution qexec =
				// QueryExecutionFactory.sparqlService(source.getSparqlEndpoint(),
				// pss.asQuery());
				QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), m);
				ResultSet rs = qexec.execSelect();
				while (rs.hasNext()) {
					QuerySolution querySolution = (QuerySolution) rs.next();
					logger.trace(querySolution.getResource("resource").getURI());
					additionalResources.add(querySolution.getResource("resource").getURI());
				}
			}
		}

		logger.trace("getting information of additional resource");
		if (!source.isUseOnlyConstruct()) {
			for (String r : additionalResources) {
				m.add(getDataRelatedToRelatedToResource(r, source, true));
			}
		}

		logger.trace("adding triples from queries");
		for (String pattern : source.getQueryToAdd()) {
			ParameterizedSparqlString pss = new ParameterizedSparqlString(pattern);
			pss.setIri("base", resourceToGet);
			// logger.trace("Query \n{}",
			// pss.asQuery().toStrisng(Syntax.defaultQuerySyntax));
			QueryExecution qexec = QueryExecutionFactory.sparqlService(source.getSparqlEndpoint(), pss.asQuery());
			Model mr = qexec.execConstruct();
			logger.trace(String.format("Triples returned {}", mr.size()));
			m.add(mr);
		}

		// adding triples from other sources
		logger.trace("Adding from secondary resources");
		for (LODSecondarySource ss : source.getSecondarySources()) {

			// Prendi Risorsa riferita dalla sorgente secondaria
			String queryToGetRefIdentifier = "SELECT DISTINCT ?refResource { "
					+ ss.getPatternToIdentifyURIPointedToExternalSource() + "}";
			ParameterizedSparqlString pssToGetRefIdentifier = new ParameterizedSparqlString(queryToGetRefIdentifier);
			pssToGetRefIdentifier.setIri("resource", resourceToGet);
			logger.info(String.format("Executing {} \non {}",
					pssToGetRefIdentifier.asQuery().toString(Syntax.syntaxSPARQL_11)));
			QueryExecution qexec = QueryExecutionFactory.sparqlService(source.getSparqlEndpoint(),
					pssToGetRefIdentifier.asQuery());
			ResultSet rs = qexec.execSelect();
			String refResource = resourceToGet;
			while (rs.hasNext()) {
				QuerySolution querySolution = (QuerySolution) rs.next();
				refResource = querySolution.get("refResource").asResource().getURI();
			}
			logger.trace(String.format("Ref Resource {}", refResource));

			// Prendi dati dalla sorgente secondaria
			for (String queryForSecondaryResoruce : ss.getQueries()) {
				ParameterizedSparqlString pssForSecondaryResource = new ParameterizedSparqlString(
						queryForSecondaryResoruce);
				pssForSecondaryResource.setIri("refResource", refResource);
				logger.trace(String.format("Executing \n{}\non {}",
						pssForSecondaryResource.asQuery().toString(Syntax.syntaxSPARQL_11), ss.getSparqlEndpoint()));
				QueryExecution qexecQueryForSecondaryResource = QueryExecutionFactory
						.sparqlService(ss.getSparqlEndpoint(), pssForSecondaryResource.asQuery());
				Model toAdd = qexecQueryForSecondaryResource.execConstruct();
				logger.trace(String.format("Number of triples from external resource {}", toAdd.size()));
				m.add(toAdd);
			}
		}

		String filename = FilenameUtils.getName(resourceToGet);
		String filePath = folderPath + "/" + filename + ".rdf";

		m.write(new FileOutputStream(new File(filePath)), "RDF/XML");

		logger.trace(String.format("{} written!", filePath));

		return filePath;
	}

	private static Model getDataRelatedToRelatedToResource(String resourceToGet, LODPrimarySource source,
			boolean reduce) {
		ParameterizedSparqlString pss = null;
		if (!reduce) {
			// @f:off
			pss = new ParameterizedSparqlString("" + "CONSTRUCT {" + "?resource ?p ?o ." + "?o ?p1 ?o2 ." + "}"
					+ "WHERE  { " + "?resource ?p ?o ." + "OPTIONAL {?o ?p1 ?o2 }" + "} ");
			// @f:on
		} else {
			// @f:off
			pss = new ParameterizedSparqlString(
					"" + "CONSTRUCT {" + "?resource ?p ?o ." + "}" + "WHERE  { " + "?resource ?p ?o ." + "} ");
			// @f:on
		}
		pss.setIri("resource", resourceToGet);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(source.getSparqlEndpoint(), pss.asQuery());
		qexec.setTimeout(TIMEOUT_1_MINUTES, TimeUnit.MINUTES, TIMEOUT_2_MINUTES, TimeUnit.MINUTES);

		logger.trace(String.format("Executing\n{}\non {}, timeout1 {} timeout2 {}",
				pss.asQuery().toString(Syntax.defaultQuerySyntax), source.getSparqlEndpoint(), qexec.getTimeout1(),
				qexec.getTimeout2()));
		Model m = qexec.execConstruct();
		m.setNsPrefixes(getPrefixes());
		logger.trace("Done!");

		return m;
	}

	private static Set<String> getResourcesOfAType(LODPrimarySource source) throws InterruptedException {

		// logger.trace("FILTER {}", source.getFilter());

		Set<String> result = new HashSet<>();

		ParameterizedSparqlString pss_count = null;

		if (source.getGraph() != null) {
			// @f:off
			pss_count = new ParameterizedSparqlString("" + "SELECT (COUNT(DISTINCT ?resource) AS ?c ) " + "FROM ?graph "
					+ "WHERE  { " + "?resource a ?type . " + source.getFilter() + "} ");
			// @f:on
			pss_count.setIri("graph", source.getGraph());
		} else {
			// @f:off
			pss_count = new ParameterizedSparqlString("" + "SELECT (COUNT(DISTINCT ?resource) AS ?c ) " + "WHERE  { "
					+ "?resource a ?type . " + source.getFilter() + "} ");
			// @f:on
		}

		pss_count.setIri("type", source.getKlass());

		logger.trace(String.format("Sparql endpoint: {}", source.getSparqlEndpoint()));
		logger.trace(String.format("Executing\n{}", pss_count.asQuery().toString(Syntax.defaultQuerySyntax)));

		QueryExecution qexec_count = QueryExecutionFactory.sparqlService(source.getSparqlEndpoint(),
				QueryFactory.create(pss_count.toString(), Syntax.syntaxSPARQL_11));

		ResultSet rs_count = qexec_count.execSelect();
		int count = Integer.parseInt(rs_count.next().getLiteral("c").getValue().toString());

		logger.info(String.format("Retrieved {} {} ", count, source.getKlass()));

		int numberOfResourcesPerQuery = 10000;
		ParameterizedSparqlString pss = null;
		if (source.getGraph() != null) {
			// @f:off
			pss = new ParameterizedSparqlString("" + "SELECT DISTINCT ?resource " + "FROM ?graph " + "WHERE  { "
					+ "?resource a ?type . " + source.getFilter() + "} " + "LIMIT ?l " + "OFFSET ?off " + "");
			// @f:on
			pss.setIri("graph", source.getGraph());
		} else {
			// @f:off
			pss = new ParameterizedSparqlString("" + "SELECT DISTINCT  ?resource " + "WHERE  { "
					+ "?resource a ?type . " + source.getFilter() + "} " + "LIMIT ?l " + "OFFSET ?off " + "");
			// @f:on
		}

		pss.setIri("type", source.getKlass());
		pss.setLiteral("l", numberOfResourcesPerQuery);

		for (int resources = 0; resources * numberOfResourcesPerQuery < count; resources++) {

			pss.setLiteral("off", (resources * numberOfResourcesPerQuery));

			for (int i = 0; i < NUMBER_OF_ATTEMPTS; i++) {
				try {
					QueryExecution qexec = QueryExecutionFactory.sparqlService(source.getSparqlEndpoint(),
							pss.asQuery());
					// logger.trace("Executing \n\n{}\n\n on {} timeout {}",
					// pss.asQuery().toString(Syntax.defaultQuerySyntax),
					// source.getSparqlEndpoint(), qexec.getTimeout1());
					ResultSet results = qexec.execSelect();
					logger.trace("Select executed!");
					while (results.hasNext()) {
						QuerySolution querySolution = (QuerySolution) results.next();
						result.add(querySolution.getResource("resource").getURI());
					}
					qexec.close();
					break;
				} catch (Exception e) {
					logger.error(String.format("Error in query excecution {}", e.getMessage()));
					Thread.sleep(SLEEP_RETRY);
				}
			}

		}

		return result;
	}

	private static Set<String> getResourcesUsingSparql(LODPrimarySource source) throws InterruptedException {
		logger.info(String.format("Getting resources using sparql %s",source.getSparqlResourceSelector()));

		Set<String> result = new HashSet<>();

		QueryExecution qexec = QueryExecutionFactory.sparqlService(source.getSparqlEndpoint(),
				QueryFactory.create(source.getSparqlResourceSelector(), Syntax.syntaxSPARQL_11));
		ResultSet rs = qexec.execSelect();
		while (rs.hasNext()) {
			QuerySolution querySolution = rs.next();
			result.add(querySolution.get("resource").asResource().getURI());
		}

		return result;
	}

	private static Map<String, String> getPrefixes() {
		Map<String, String> prefixes = new HashMap<>();

		// B
		prefixes.put("bpr", "http://dati.camera.it/ocd/bpr/");
		prefixes.put("bibo", "http://purl.org/ontology/bibo/");
		prefixes.put("bio", "http://purl.org/vocab/bio/0.1/");

		// C
		prefixes.put("ccd", "http://dati.camera.it/ocd/bpr/ccd.rdf/");

		// D
		prefixes.put("dc", "http://purl.org/dc/elements/1.1/");
		prefixes.put("dawgt", "http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#");
		prefixes.put("dbpedia", "http://dbpedia.org/resource/");
		prefixes.put("dbpprop", "http://dbpedia.org/property/");
		prefixes.put("dcterms", "http://purl.org/dc/terms/");

		// F
		prefixes.put("fn", "http://www.w3.org/2005/xpath-functions/#");
		prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");

		// G
		prefixes.put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
		prefixes.put("go", "http://purl.org/obo/owl/GO#");

		// I
		prefixes.put("isbd", "http://iflastandards.info/ns/isbd/elements/");

		// M
		prefixes.put("math", "http://www.w3.org/2000/10/swap/math#");
		prefixes.put("mesh", "http://purl.org/commons/record/mesh/");
		prefixes.put("mf", "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#");
		prefixes.put("mandatocamera", "http://dati.camera.it/ocd/mandatoCamera.rdf/");

		// N
		prefixes.put("nci", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#");

		// O
		prefixes.put("obo", "http://www.geneontology.org/formats/oboInOwl#");
		prefixes.put("ocd", "http://dati.camera.it/ocd/");
		prefixes.put("ocdleg", "http://dati.camera.it/ocd/legislatura.rdf/");
		prefixes.put("ods", "http://lod.xdams.org/ontologies/ods/");
		prefixes.put("org", "http://www.w3.org/ns/org#");
		prefixes.put("owl", "http://www.w3.org/2002/07/owl#");
		prefixes.put("ocdpersona", "http://dati.camera.it/ocd/persona.rdf/");
		prefixes.put("ocdluogo", "http://dati.camera.it/ocd/luogo.rdf/");
		prefixes.put("ocdelezione", "http://dati.camera.it/ocd/elezione.rdf/");
		prefixes.put("ocdmandatosenato", "http://dati.camera.it/ocd/mandatoSenato.rdf/");
		prefixes.put("ocddeputato", "http://dati.camera.it/ocd/deputato.rdf/");
		prefixes.put("ocdsenatore", "http://dati.camera.it/ocd/senatore.rdf/");
		prefixes.put("ocdccd", "http://dati.camera.it/ocd/bpr/ccd.rdf/");
		prefixes.put("ocdperiodico", "http://dati.camera.it/ocd/bpr/periodico.rdf/");

		// P
		prefixes.put("product", "http://www.buy.com/rss/module/productV2/");
		prefixes.put("protseq", "http://purl.org/science/protein/bysequence/");
		prefixes.put("protseq", "http://purl.org/science/protein/bysequence/");
		prefixes.put("pimcontact", "http://www.w3.org/2000/10/swap/pim/contact#");

		// R
		prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		prefixes.put("rdfa", "http://www.w3.org/ns/rdfa#");
		prefixes.put("rdfdf", "http://www.openlinksw.com/virtrdf-data-formats#");

		// S
		prefixes.put("skos", "http://www.w3.org/2008/05/skos#");
		prefixes.put("sc", "http://purl.org/science/owl/sciencecommons/");
		prefixes.put("scovo", "http://purl.org/NET/scovo#");
		prefixes.put("sd", "http://www.w3.org/ns/sparql-service-description#");
		prefixes.put("sioc", "http://rdfs.org/sioc/ns#");
		prefixes.put("swvocabns", "http://www.w3.org/2003/06/sw-vocab-status/ns#");
		prefixes.put("schemaorg", "http://schema.org/");

		// T
		prefixes.put("test", "http://dati.camera.it/ocd/test");

		// V
		prefixes.put("vcard", "http://www.w3.org/2001/vcard-rdf/3.0#");
		prefixes.put("vcard2006", "http://www.w3.org/2006/vcard/ns#");
		prefixes.put("virtcxml", "http://www.openlinksw.com/schemas/virtcxml#");
		prefixes.put("virtrdf", "http://www.openlinksw.com/schemas/virtrdf#");
		prefixes.put("void", "http://rdfs.org/ns/void#");
		prefixes.put("virtcxml", "http://www.openlinksw.com/schemas/virtcxml#");
		prefixes.put("void", "http://rdfs.org/ns/void#");

		// X
		prefixes.put("xf", "http://www.w3.org/2004/07/xpath-functions");
		prefixes.put("xml", "http://www.w3.org/XML/1998/namespace");
		prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		prefixes.put("xsl10", "http://www.w3.org/XSL/Transform/1.0");
		prefixes.put("xsl1999", "http://www.w3.org/1999/XSL/Transform");
		prefixes.put("xslwd", "http://www.w3.org/TR/WD-xsl");

		// Y
		prefixes.put("yago", "http://dbpedia.org/class/yago/");

		return prefixes;
	}

}
