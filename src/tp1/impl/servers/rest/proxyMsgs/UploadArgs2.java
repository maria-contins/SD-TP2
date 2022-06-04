package tp1.impl.servers.rest.proxyMsgs;

public class UploadArgs2 {
		final String mode;
		final boolean autorename, mute, strict_conflict;
		final String path;
		
		public UploadArgs2(String path, boolean overwrite) {
			this.mode = overwrite ? "overwrite" : "add";
			this.path = path;
			this.autorename = false;
			this.mute = false;
			this.strict_conflict = false;
		}	
}