package it.cnr.istc.stlab.pss.harvester;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HarvesterConfiguration {

	private static Logger logger = LoggerFactory.getLogger(HarvesterConfiguration.class);
	private static HarvesterConfiguration instance;
	private String taskFile, privatePathKey;
	private boolean excludeSSH = false;

	private static String CONFIGURATION_FILE = "config.properties";

	private HarvesterConfiguration() {
		try {
			Configurations configs = new Configurations();
			Configuration config = configs.properties(CONFIGURATION_FILE);
			this.taskFile = config.getString("taskFile");
			this.privatePathKey = config.getString("privatePathKey");
			this.excludeSSH = config.getBoolean("excludeSSH");

			logger.info("Configuration file {}\n{}", CONFIGURATION_FILE, toString());

		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static void setConfigFile(String configFilePath) {
		CONFIGURATION_FILE = configFilePath;
	}

	public String getPrivatePathKey() {
		return privatePathKey;
	}

	public static HarvesterConfiguration getPSSConfiguration() {
		if (instance == null) {
			instance = new HarvesterConfiguration();
		}
		return instance;
	}

	public String getTaskFile() {
		return taskFile;
	}
	
	public boolean excludeSSH() {
		return excludeSSH;
	}

	@Override
	public String toString() {
		return "HarvesterConfiguration [taskFile=" + taskFile + "]";
	}

}
