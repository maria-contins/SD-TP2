package tp1.api.service.java;

import java.util.List;

import tp1.api.FileInfo;

public interface Directory {

	static String SERVICE_NAME = "directory";
	
	Result<FileInfo> writeFile(Long version, String filename, byte[] data, String userId, String password);

	Result<Void> deleteFile(Long version ,String filename, String userId, String password);

	Result<Void> shareFile(Long version,String filename, String userId, String userIdShare, String password);

	Result<Void> unshareFile(Long version, String filename, String userId, String userIdShare, String password);

	Result<byte[]> getFile(Long version, String filename,  String userId, String accUserId, String password);

	Result<List<FileInfo>> lsFile(String userId, String password);
		
	Result<Void> deleteUserFiles(String userId, String password, String token);
}
