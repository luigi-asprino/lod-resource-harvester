package it.cnr.istc.stlab.pss.harvester;

public class RemoteDestination {

	private String host, user, password, folderPath;

	public RemoteDestination(String host, String user, String password, String folderPath) {
		super();
		this.host = host;
		this.user = user;
		this.password = password;
		this.folderPath = folderPath;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFolderPath() {
		return folderPath;
	}

	public void setFolderPath(String folderPath) {
		this.folderPath = folderPath;
	}

}
