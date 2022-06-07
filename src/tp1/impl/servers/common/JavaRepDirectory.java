package tp1.impl.servers.common;


import tp1.api.FileInfo;
import tp1.api.User;
import tp1.api.service.java.DirectoryRep;
import tp1.api.service.java.Result;
import tp1.impl.kafka.KafkaEvent;
import tp1.impl.kafka.TotalOrderExecutor;
import util.JSON;
import util.Token;

import java.net.URI;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static tp1.api.service.java.Result.ErrorCode.BAD_REQUEST;
import static tp1.api.service.java.Result.error;
import static tp1.api.service.java.Result.ok;
import static tp1.impl.clients.Clients.FilesClients;


public class JavaRepDirectory extends JavaDirectory implements DirectoryRep {

    public JavaRepDirectory(){
    }

    /*private static boolean badParam(String str) {
        return str == null || str.length() == 0;
    }

    private Result<User> getUser(String userId, String password) {
        try {
            return users.get( new UserInfo( userId, password));
        } catch( Exception x ) {
            x.printStackTrace();
            return error( Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    /*private Queue<URI> orderCandidateFileServers(ExtendedFileInfo file) {
        //int MAX_SIZE=3;
        int MAX_SIZE= Math.max(FilesClients.all().size()-1, 1);

        Queue<URI> result = new ArrayDeque<>();

        if( file != null ){
            result.addAll(file.allUris());
        }


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
    }*/



    public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password, TotalOrderExecutor toe,String write) {

        if (badParam(filename) || badParam(userId))
            return error(BAD_REQUEST);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        boolean gotRequest = false;
        /*var uf = userFiles.computeIfAbsent(userId, (k) -> new UserFiles());
        synchronized (uf) {*/
            var fileId = fileId(filename, userId);
            var file = super.files.get(fileId);

            var info = file != null ? file.info() : new FileInfo();

            Result<Void> result = null;
            List<URI> uris = new LinkedList<>();
            for (var uri :  orderCandidateFileServers(file)) {//COLOCAR FORMA DE PARAR CICLO APOS TER ESCRITO EM N-1 SERVIDORES
                result = FilesClients.get(uri).writeFile(fileId, data, createToken(fileId));
                if (result.isOK()){
                    gotRequest = true;
                    uris.add(uri);
                    info.setOwner(userId);
                    info.setFilename(filename);
                    info.setFileURL(String.format("%s/files/%s", uri, fileId));
                } else
                    Log.info(String.format("Files.writeFile(...) to %s failed with: %s \n", uri, result));
            }
            System.out.println(uris);
            //assert result != null;

            super.files.put(fileId, file = new ExtendedFileInfo(uris, fileId, info));

            /*if( uf.owned().add(fileId)){
                for(URI uri : file.allUris)
                    getFileCounts(uri, true).numFiles().incrementAndGet();
            }*/
            //Log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + file.toString());

            Map<String, String> eventInfo = new HashMap<>();
            eventInfo.put("filename", JSON.encode(filename));
            eventInfo.put("userId",JSON.encode(userId));
            eventInfo.put("fileId",JSON.encode(fileId));
            eventInfo.put("uris", JSON.encode(uris.toArray()));
            eventInfo.put("info", JSON.encode(info));

            toe.publish(write, JSON.encode(eventInfo));

            if(gotRequest)
                return ok(file.info());

            /*String[] operationInfo = {filename, userId, String.valueOf(uris)};
            int len1 = operationInfo.length;
            int len2 = uris.size();
            String[] finalArray = new String[len1 + len2];
            System.arraycopy(operationInfo,0,finalArray,0,len1);

            for(URI uri : uris){
                finalArray[len1++] = uri.toString();
            }*/



            return error(BAD_REQUEST);
        //}
    }

    public Result<Void> writeFileEvent(String filename, String userId, ExtendedFileInfo extFileInfo /*, ExtendedFileInfo record*/) {//TODO REMOVER LISTA DE URIS

        var uf = super.userFiles.computeIfAbsent(userId, (k) -> new UserFiles());

        synchronized (uf) {
            String fileId = fileId(filename, userId);


            ExtendedFileInfo ef =  super.files.put(fileId, extFileInfo);

            Log.info("written " + ((ef == null) ? "new" : "old") +  " -  FICHEIRO " + fileId + " COLOCADO COM SUCESSO = " + extFileInfo);

            if( uf.owned().add(fileId)){
                for(URI uri : extFileInfo.allUris())
                    getFileCounts(uri, true).numFiles().incrementAndGet();
            }

            Log.info("MAP: " + super.files.toString());
        }
        return ok();
    }

    public String deleteFileEvent(String filename, String userId) {

        var fileId = fileId(filename, userId);

        var uf = super.userFiles.getOrDefault(userId, new UserFiles());
        synchronized (uf) {
            var info = super.files.remove(fileId);
            uf.owned().remove(fileId);
        }

        //getFileCounts(info.uri(), false).numFiles().decrementAndGet(); //TODO FIX THIS BULLSHIT

        return null;
    }

    public String shareFileEvent(String filename, String userId, String userIdShare){

        var fileId = fileId(filename, userId);
        var file = super.files.get(fileId);

        var uf = super.userFiles.computeIfAbsent(userIdShare, (k) -> new UserFiles());
        synchronized (uf) {
            uf.shared().add(fileId);
            file.info().getSharedWith().add(userIdShare);
        }

        return null;
    }

    public String unshareFile(String filename, String userId, String userIdShare){

        var fileId = fileId(filename, userId);
        var file = super.files.get(fileId);

        var uf = super.userFiles.computeIfAbsent(userIdShare, (k) -> new UserFiles());
        synchronized (uf) {
            uf.shared().remove(fileId);
            file.info().getSharedWith().remove(userIdShare);
        }

        return null;
    }

}
