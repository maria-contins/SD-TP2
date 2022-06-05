package tp1.impl.clients.common;

import java.util.List;

import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;

public class RetryDirectoryClient extends RetryClient implements Directory {

	final Directory impl;

	public RetryDirectoryClient( Directory impl ) {
		this.impl = impl;	
	}

	@Override
	public Result<FileInfo> writeFile(Long version, String filename, byte[] data, String userId, String password) {
		return super.reTry( ()-> impl.writeFile(version, filename, data, userId, password));
	}

	@Override
	public Result<Void> deleteFile(Long version, String filename, String userId, String password) {
		return super.reTry( ()-> impl.deleteFile(version, filename, userId, password));
		
	}

	@Override
	public Result<Void> shareFile(Long version, String filename, String userId, String userIdShare, String password) {
		return super.reTry( ()-> impl.shareFile(version, filename, userId, userIdShare, password));
	}

	@Override
	public Result<Void> unshareFile(Long version, String filename, String userId, String userIdShare, String password) {
		return super.reTry( ()-> impl.unshareFile(version, filename, userId, userIdShare, password));
	}

	@Override
	public Result<byte[]> getFile(Long version, String filename, String userId, String accUserId, String password) {
		return super.reTry( ()-> impl.getFile(version, filename, userId, accUserId, password));
	}

	@Override
	public Result<List<FileInfo>> lsFile(String userId, String password) {
		return super.reTry( ()-> impl.lsFile(userId, password));
	}

	@Override
	public Result<Void> deleteUserFiles(String userId, String password, String token) {
		return super.reTry( ()-> impl.deleteUserFiles(userId, password, token));
	}
	
}
