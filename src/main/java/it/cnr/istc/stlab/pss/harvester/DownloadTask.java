package it.cnr.istc.stlab.pss.harvester;

public class DownloadTask {

	private LODPrimarySource source;
	private int limit;
	private LocalDestination localDestination;
	private RemoteDestination remoteDestination;

	public DownloadTask(LODPrimarySource source, LocalDestination localDestination, int limit) {
		this(source, localDestination, null, limit);
	}

	public DownloadTask(LODPrimarySource source, LocalDestination localDestination, RemoteDestination remoteDestination, int limit) {
		super();
		this.source = source;
		this.localDestination = localDestination;
		this.remoteDestination = remoteDestination;
		this.limit = limit;
	}

	public LODPrimarySource getSource() {
		return source;
	}

	public void setSource(LODPrimarySource source) {
		this.source = source;
	}

	public LocalDestination getLocalDestination() {
		return localDestination;
	}

	public void setLocalDestination(LocalDestination localDestination) {
		this.localDestination = localDestination;
	}

	public RemoteDestination getRemoteDestination() {
		return remoteDestination;
	}

	public void setRemoteDestination(RemoteDestination remoteDestination) {
		this.remoteDestination = remoteDestination;
	}
	
	public int getLimit() {
		return limit;
	}

}
