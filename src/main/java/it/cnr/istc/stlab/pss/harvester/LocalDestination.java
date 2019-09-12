package it.cnr.istc.stlab.pss.harvester;

public class LocalDestination {

	private String localPath;

	public LocalDestination(String localPath) {
		super();
		this.localPath = localPath;
	}

	public String getLocalPath() {
		return localPath;
	}

	public String getDownloadedFile() {
		return localPath + "/downloaded.txt";
	}
	
	public String getResourcesImpossibleToDownload() {
		return localPath + "/undownloadable.txt";
	}
	
	public String getUploadedFile() {
		return localPath + "/uploaded.txt";
	}

}
