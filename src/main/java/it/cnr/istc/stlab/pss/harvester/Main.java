package it.cnr.istc.stlab.pss.harvester;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class Main {

	private static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		try {
			if (args.length > 0) {
				logger.info("Configuration File {}", args[0]);
				HarvesterConfiguration.setConfigFile(args[0]);
			}
			Harvester.harvest(TaskBuilder.getTasks());
			logger.info("Harvesting completed");
		} catch (IOException | JSchException | SftpException | InterruptedException e) {
			e.printStackTrace();
		}
	}

}
