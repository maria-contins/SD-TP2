package tp1.impl.servers.rest.proxyMsgs;

public class ListFolderContinueArgs {
	final String cursor;
	
	public ListFolderContinueArgs(String cursor) {
		this.cursor = cursor;
	}	
}