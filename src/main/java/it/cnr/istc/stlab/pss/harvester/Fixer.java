package it.cnr.istc.stlab.pss.harvester;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

public class Fixer {

	private static void fixFolder(File folderIn, String folderOut) throws FileNotFoundException {
		new File(folderOut).mkdirs();
		System.out.println("Processing folder " + folderIn.getAbsolutePath());
		int c = 0;
		for (File f : folderIn.listFiles()) {
			if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("rdf")) {
				Model m = ModelFactory.createDefaultModel();
				RDFDataMgr.read(m, f.getAbsolutePath());
				ResIterator it = m.listSubjects();
				boolean found = false;
				String uri = null;
				while (it.hasNext()) {
					Resource s = (Resource) it.next();
					if (s != null && !s.isAnon() && s.getURI().contains(FilenameUtils.getBaseName(f.getName()))) {
						uri = s.getURI();
						if (found) {
							c++;
							System.out.println("Multiple " + s.getURI());
						}
					}
				}
				m.add(m.createResource(uri), RDF.type, m.createResource("https://w3id.org/pss/CrawledResource"));
				m.write(new FileOutputStream(new File(folderOut + "/" + FilenameUtils.getBaseName(f.getName()) + ".rdf")));
			}
		}
		System.out.println("Number of already found within " + folderIn.getAbsolutePath() + " : " + c);
	}

	public static void main(String[] args) throws FileNotFoundException {
		File folderFile = new File(args[0]);
		for (File f : folderFile.listFiles()) {
			fixFolder(f, args[1] + "/" + f.getName());
		}
	}

}
