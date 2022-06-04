package tp1.impl.servers.rest.proxyMsgs;

import java.util.List;

public class DeleteArgs {
	List<String> entries;
	
	public DeleteArgs(List<String> entries) {
		this.entries = entries;
	}
}