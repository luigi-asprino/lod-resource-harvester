# LOD Resource Harvester (LOD-RH)

Given a URL of a SPARQL endpoint and a query to select a some resources, LOD-RH downloads RDF subject pages for selected resources.


### Dependency

Before using LOD-RH make sure that [lgu-commons](https://github.com/luigi-asprino/lgu-commons) is installed on your computer.

### Installation

You can install LOD-RH using maven.

1. Checkout repository
```
$ git clone https://github.com/luigi-asprino/lod-resource-harvester.git
```
2. Compile the project with maven
```
cd lod-resource-harvester/
mvn clean install
```
3. Import LOD-RH into your project by adding the following dependency to pom.xml of your project.
```
<dependency>
	<groupId>it.cnr.istc.stlab</groupId>
	<artifactId>pss.harvester</artifactId>
	<version>0.0.1</version>
</dependency>
```

### Usage

1. Define the harvesting task (e.g. Download subject pages of the first 10 persons in DBpedia) and save it in a text (JSON) file.

```
{
	"tasks": [
		{
			"endpoint": "http://dbpedia.org/sparql/",
			"sparqlResourceSelector": "prefix foaf: <http://xmlns.com/foaf/0.1/> select distinct ?resource {?resource a foaf:Person} LIMIT 10",
			"localDestination": "DBpedia_Persons"
		}
	]
}

```

2. Provide the path to the JSON file in the configuration file.

```
taskFile=src/main/resources/tasks_dbpedia.json
```

3.  Run

```
public static void main(String[] args) {
	try {
		HarvesterConfiguration.setConfigFile("/path/to/config/file");
		Harvester.harvest(TaskBuilder.getTasks());
	} catch (IOException | JSchException | SftpException | InterruptedException e) {
		e.printStackTrace();
	}
}
```

### Run from terminal

You can run LOD Resource Harvester from terminal by running the following command

```
mvn exec:java -Dexec.mainClass="it.cnr.istc.stlab.pss.harvester.Main" -Dexec.args="src/main/resources/tasks_dbpedia.json"
```

### License

LOD Resource Harvester is distributed under license 
