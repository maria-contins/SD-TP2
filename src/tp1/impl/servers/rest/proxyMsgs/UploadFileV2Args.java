package tp1.impl.servers.rest.proxyMsgs;

public record UploadFileV2Args(String path, boolean overwrite, boolean autorename, boolean mute, boolean strict_conflict) {
}
