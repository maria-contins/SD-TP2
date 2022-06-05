package tp1.impl.servers.common;

import static java.lang.System.currentTimeMillis;
import static tp1.api.service.java.Result.error;
import static tp1.api.service.java.Result.ok;
import static tp1.api.service.java.Result.ErrorCode.INTERNAL_ERROR;
import static tp1.api.service.java.Result.ErrorCode.NOT_FOUND;
import static tp1.api.service.java.Result.ErrorCode.FORBIDDEN;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;

import tp1.api.service.java.Files;
import tp1.api.service.java.Result;
import util.IO;
import util.Token;

public class JavaFiles implements Files {

	static final String DELIMITER = "$$$";
	private static final String ROOT = "/tmp/";
	
	public JavaFiles() {
		new File( ROOT ).mkdirs();
	}

	@Override
	public Result<byte[]> getFile(String fileId, String token) {

		if(invalidTokenDir(token, fileId))
			return error(FORBIDDEN);

		fileId = fileId.replace( DELIMITER, "/");
		byte[] data = IO.read( new File( ROOT + fileId ));
		return data != null ? ok( data) : error( NOT_FOUND );
	}

	@Override
	public Result<Void> deleteFile(String fileId, String token) {

		if(invalidTokenDir(token, fileId))
			return error(FORBIDDEN);

		fileId = fileId.replace( DELIMITER, "/");
		boolean res = IO.delete( new File( ROOT + fileId ));	
		return res ? ok() : error( NOT_FOUND );
	}

	@Override
	public Result<Void> writeFile(String fileId, byte[] data, String token) {

		if(invalidTokenDir(token, fileId))
			return error(FORBIDDEN);

		fileId = fileId.replace( DELIMITER, "/");
		File file = new File(ROOT + fileId);
		file.getParentFile().mkdirs();
		IO.write( file, data);
		return ok();
	}
	

	@Override
	public Result<Void> deleteUserFiles(String userId, String token) {

		if(invalidTokenUsers(token))
			return error(FORBIDDEN);

		File file = new File(ROOT + userId);
		try {
			java.nio.file.Files.walk(file.toPath())
			.sorted(Comparator.reverseOrder())
			.map(Path::toFile)
			.forEach(File::delete);
		} catch (IOException e) {
			e.printStackTrace();
			return error(INTERNAL_ERROR);
		}
		return ok();
	}

	public static String fileId(String filename, String userId) {
		return userId + JavaFiles.DELIMITER + filename;
	}

	private boolean invalidTokenDir(String token, String fileId){
		String[] tokenInfo = token.split("\\?\\?");

		String time = tokenInfo[1];
		String mySecret = Token.get();

		if (Long.parseLong(time) < currentTimeMillis())
			return true;

		long hashed = (fileId + time + mySecret).hashCode();

		return hashed != Long.parseLong(tokenInfo[2]);
	}

	private boolean invalidTokenUsers(String token) {
		String[] tokenInfo = token.split("\\?\\?");

		String time = tokenInfo[0];
		String mySecret = Token.get();

		if (Long.parseLong(time) < currentTimeMillis())
			return true;

		long hashed = (time + mySecret).hashCode();

		return hashed != Long.parseLong(tokenInfo[1]);
	}
}
