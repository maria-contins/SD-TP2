package tp1.impl.servers.common;

import static java.lang.System.currentTimeMillis;
import static tp1.api.service.java.Result.error;
import static tp1.api.service.java.Result.ok;
import static tp1.api.service.java.Result.redirect;
import static tp1.api.service.java.Result.ErrorCode.BAD_REQUEST;
import static tp1.api.service.java.Result.ErrorCode.FORBIDDEN;
import static tp1.api.service.java.Result.ErrorCode.NOT_FOUND;
import static tp1.impl.clients.Clients.FilesClients;
import static tp1.impl.clients.Clients.UsersClients;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import tp1.api.FileInfo;
import tp1.api.User;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;
import tp1.api.service.java.Result.ErrorCode;
import tp1.impl.discovery.Discovery;
import util.Token;

public class JavaDirectory implements Directory {

	static final long USER_CACHE_EXPIRATION = 3000;

	final LoadingCache<UserInfo, Result<User>> users = CacheBuilder.newBuilder()
			.expireAfterWrite( Duration.ofMillis(USER_CACHE_EXPIRATION))
			.build(new CacheLoader<>() {
				@Override
				public Result<User> load(UserInfo info) throws Exception {
					var res = UsersClients.get().getUser( info.userId(), info.password());
					if( res.error() == ErrorCode.TIMEOUT)
						return error(BAD_REQUEST);
					else
						return res;
				}
			});

	final static Logger Log = Logger.getLogger(JavaDirectory.class.getName());
	final ExecutorService executor = Executors.newCachedThreadPool();

	final Map<String, ExtendedFileInfo> files = new ConcurrentHashMap<>();
	final Map<String, UserFiles> userFiles = new ConcurrentHashMap<>();
	final Map<URI, FileCounts> fileCounts = new ConcurrentHashMap<>();

	@Override
	public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password) {

		if (badParam(filename) || badParam(userId))
			return error(BAD_REQUEST);

		var user = getUser(userId, password);
		if (!user.isOK())
			return error(user.error());

		boolean gotRequest = false;
		var uf = userFiles.computeIfAbsent(userId, (k) -> new UserFiles());
		synchronized (uf) {
			var fileId = fileId(filename, userId);
			var file = files.get(fileId);

			var info = file != null ? file.info() : new FileInfo();

			Result<Void> result = null;
			int count = 0;
			List<URI> uris = new LinkedList<>();
			for (var uri :  orderCandidateFileServers(file)) {
				result = FilesClients.get(uri).writeFile(fileId, data, /*Token.get()*/ createToken(fileId));
				if (result.isOK()){
					count++;
					gotRequest = true;
					uris.add(uri);
					info.setOwner(userId);
					info.setFilename(filename);
					info.setFileURL(String.format("%s/files/%s", uri, fileId));
				} else
					Log.info(String.format("Files.writeFile(...) to %s failed with: %s \n", uri, result));
				if (count == FilesClients.all().size()-1)
					break;
			}
			System.out.println(uris);
			//assert result != null;

			files.put(fileId, file = new ExtendedFileInfo(uris, fileId, info));

			if( uf.owned().add(fileId)){
				for(URI uri : file.allUris)
					getFileCounts(uri, true).numFiles().incrementAndGet();
			}

			if(gotRequest)
				return ok(file.info());

			return error(BAD_REQUEST);
		}
	}


	@Override
	public Result<Void> deleteFile(String filename, String userId, String password) {
		if (badParam(filename) || badParam(userId))
			return error(BAD_REQUEST);

		var fileId = fileId(filename, userId);

		var file = files.get(fileId);
		if (file == null)
			return error(NOT_FOUND);

		var user = getUser(userId, password);
		if (!user.isOK())
			return error(user.error());

		var uf = userFiles.getOrDefault(userId, new UserFiles());
		synchronized (uf) {
			var info = files.remove(fileId);
			uf.owned().remove(fileId);

			executor.execute(() -> {
				this.removeSharesOfFile(info);
				for(URI uri : file.allUris) {
					FilesClients.get(uri).deleteFile(fileId, /*password,*/ createToken(fileId));
				}

			});
			for(URI uri : file.allUris)  //TODO AQUI ANTES ERA FILE.INFO OU SEJA O URL TALVEZ O URI NAO É SUFICIENTE
				getFileCounts(uri, false).numFiles().decrementAndGet();
		}
		return ok();
	}

	@Override
	public Result<Void> shareFile(String filename, String userId, String userIdShare, String password) {
		if (badParam(filename) || badParam(userId) || badParam(userIdShare))
			return error(BAD_REQUEST);

		var fileId = fileId(filename, userId);

		var file = files.get(fileId);
		if (file == null || getUser(userIdShare, "").error() == NOT_FOUND)
			return error(NOT_FOUND);

		var user = getUser(userId, password);
		if (!user.isOK())
			return error(user.error());

		var uf = userFiles.computeIfAbsent(userIdShare, (k) -> new UserFiles());
		synchronized (uf) {
			uf.shared().add(fileId);
			file.info().getSharedWith().add(userIdShare);
		}

		return ok();
	}

	@Override
	public Result<Void> unshareFile(String filename, String userId, String userIdShare, String password) {
		if (badParam(filename) || badParam(userId) || badParam(userIdShare))
			return error(BAD_REQUEST);

		var fileId = fileId(filename, userId);

		var file = files.get(fileId);
		if (file == null || getUser(userIdShare, "").error() == NOT_FOUND)
			return error(NOT_FOUND);

		var user = getUser(userId, password);
		if (!user.isOK())
			return error(user.error());

		var uf = userFiles.computeIfAbsent(userIdShare, (k) -> new UserFiles());
		synchronized (uf) {
			uf.shared().remove(fileId);
			file.info().getSharedWith().remove(userIdShare);
		}

		return ok();
	}

	@Override
	public Result<byte[]> getFile(String filename, String userId, String accUserId, String password) {


		if (badParam(filename))
			return error(BAD_REQUEST);

		var fileId = fileId(filename, userId);
		var file = files.get(fileId);
		if (file == null)
			return error(NOT_FOUND);

		var user = getUser(accUserId, password);
		if (!user.isOK())
			return error(user.error());

		if (!file.info().hasAccess(accUserId))
			return error(FORBIDDEN);

		Result<byte[]> result = null;

		URI fileURI = files.get(fileId).allUris.get(0);

		if (fileURI.toString().contains("rest"))
			result = redirect((String.format("%s/files/%s", fileURI, fileId) + "?token=" + createToken(fileId)));
		else
			result = FilesClients.get(fileURI).getFile(fileId, createToken(fileId));

		if(!result.isOK()){
			URI first = files.get(fileId).allUris.remove(0);
			files.get(fileId).allUris.add(first);
		}  //TODO DISCOVERY TER UM MAPA,


		/*TODO CODIGO REDIRECT APRIMORADO IMPLEMENTAR NO FUTURO!
		Discovery inst = Discovery.getInstance();
		URI availableServer = inst.function(files.get(fileId).allUris);

		if (fileURI.toString().contains("rest"))
			result = redirect((String.format("%s/files/%s", fileURI, fileId) + "?token=" + createToken(fileId)));
		else
			result = FilesClients.get(fileURI).getFile(fileId, createToken(fileId));*/

		return result;
	}

	@Override
	public Result<List<FileInfo>> lsFile(String userId, String password) {
		if (badParam(userId))
			return error(BAD_REQUEST);

		var user = getUser(userId, password);
		if (!user.isOK())
			return error(user.error());

		var uf = userFiles.getOrDefault(userId, new UserFiles());
		synchronized (uf) {
			var infos = Stream.concat(uf.owned().stream(), uf.shared().stream()).map(f -> files.get(f).info())
					.collect(Collectors.toSet());

			return ok(new ArrayList<>(infos));
		}
	}

	public static String fileId(String filename, String userId) {
		return userId + JavaFiles.DELIMITER + filename;
	}

	public static boolean badParam(String str) {
		return str == null || str.length() == 0;
	}

	public Result<User> getUser(String userId, String password) {
		try {
			return users.get( new UserInfo( userId, password));
		} catch( Exception x ) {
			x.printStackTrace();
			return error( ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<Void> deleteUserFiles(String userId, String password, String token) {
		users.invalidate( new UserInfo(userId, password));

		if (invalidToken(token))
			return error(ErrorCode.FORBIDDEN);

		var fileIds = userFiles.remove(userId);
		if (fileIds != null)
			for (var id : fileIds.owned()) {
				var file = files.remove(id);
				removeSharesOfFile(file);
				for(URI uri : file.allUris)
					getFileCounts(uri, false).numFiles().decrementAndGet();
			}
		return ok();
	}

	private void removeSharesOfFile(ExtendedFileInfo file) {
		for (var userId : file.info().getSharedWith())
			userFiles.getOrDefault(userId, new UserFiles()).shared().remove(file.fileId());
	}


	public Queue<URI> orderCandidateFileServers(ExtendedFileInfo file) {
		int MAX_SIZE=3;
		//int MAX_SIZE= Math.max(FilesClients.all().size()-1, 1);

		Queue<URI> result = new ArrayDeque<>();

		if( file != null )
			result.addAll( file.allUris());

		FilesClients.all()
				.stream()
				.filter( u -> ! result.contains(u))
				.map(u -> getFileCounts(u, false))
				.sorted( FileCounts::ascending )
				.map(FileCounts::uri)
				.limit(MAX_SIZE)
				.forEach( result::add );

		while( result.size() < MAX_SIZE )
			result.add( result.peek() );

		Log.info("Candidate files servers: " + result+ "\n");
		return result;
	}

	public FileCounts getFileCounts( URI uri, boolean create ) {
		if( create )
			return fileCounts.computeIfAbsent(uri,  FileCounts::new);
		else
			return fileCounts.getOrDefault( uri, new FileCounts(uri) );
	}

	public static record ExtendedFileInfo(List<URI> allUris, String fileId, FileInfo info) {
	}

	static record UserFiles(Set<String> owned, Set<String> shared) {

		UserFiles() {
			this(ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
		}
	}

	static record FileCounts(URI uri, AtomicLong numFiles) {
		FileCounts( URI uri) {
			this(uri, new AtomicLong(0L) );
		}

		static int ascending(FileCounts a, FileCounts b) {
			return Long.compare( a.numFiles().get(), b.numFiles().get());
		}
	}

	static record UserInfo(String userId, String password) {
	}

	public String createToken(String fileId) {
		String mySecret = Token.get();
		long time = (currentTimeMillis()+10000);
		String clear = fileId+"??"+time+"??" ;
		int hashed = (fileId+time+mySecret).hashCode();

		System.out.println(clear);
		System.out.println(hashed);
		return clear+hashed;
	}

	private boolean invalidToken(String token) {
		String[] tokenInfo = token.split("\\?\\?");

		String time = tokenInfo[0];
		String mySecret = Token.get();

		if (Long.parseLong(time) < currentTimeMillis())
			return true;

		long hashed = (time + mySecret).hashCode();

		return hashed != Long.parseLong(tokenInfo[1]);
	}
}