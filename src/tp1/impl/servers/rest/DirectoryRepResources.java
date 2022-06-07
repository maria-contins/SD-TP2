package tp1.impl.servers.rest;

import tp1.api.FileInfo;

import java.net.URI;
import java.util.Arrays;
import tp1.api.service.java.DirectoryRep;
import tp1.api.service.java.Result;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.kafka.KafkaEvent;
import tp1.impl.kafka.TotalOrderExecutor;
import tp1.impl.servers.common.JavaDirectory;
import tp1.impl.servers.common.JavaRepDirectory;
import util.JSON;

import java.util.List;
import java.util.logging.Logger;

import static tp1.impl.clients.Clients.FilesClients;

public class DirectoryRepResources extends RestResource implements RestDirectory {
    private static Logger Log = Logger.getLogger(DirectoryRepResources.class.getName());

    private static final String OP_WRITE = "write";
    private static final String OP_DELETE = "delete";
    private static final String OP_SHARE = "share";
    private static final String OP_UNSHARE = "unshare";

    private static final String REST = "/rest/";

    final JavaRepDirectory impl;
    final TotalOrderExecutor toe;

    public DirectoryRepResources() {
        impl = new JavaRepDirectory();
        toe = new TotalOrderExecutor(impl);
    }

    public FileInfo writeFile(String filename, byte[] data, String userId, String password) {
        Log.info(String.format("REST writeFile: filename = %s, data.length = %d, userId = %s, password = %s \n",
                filename, data.length, userId, password));


        FileInfo result = super.resultOrThrow(impl.writeFile(filename, data, userId, password, toe , OP_WRITE));

        /*List<URI> uris = result.getUris();

        String[] operationInfo = {filename, userId, String.valueOf(result.getUris().size())};
        int len1 = operationInfo.length;
        int len2 = uris.size();
        String[] finalArray = new String[len1 + len2];
        System.arraycopy(operationInfo,0,finalArray,0,len1);

        for(URI uri : uris){
            finalArray[len1++] = uri.toString();
        }

        toe.publish(OP_WRITE, JSON.encode(finalArray));*/
        return result;
    }

    @Override
    public void deleteFile(String filename, String userId, String password) {
        Log.info(String.format("REST deleteFile: filename = %s, userId = %s, password =%s\n", filename, userId,
                password));

        super.resultOrThrow(impl.deleteFile(filename, userId, password));

        String[] operationInfo = {filename, userId};

        toe.publish(OP_DELETE, JSON.encode(operationInfo));
    }

    @Override
    public void shareFile(String filename, String userId, String userIdShare, String password) {
        Log.info(String.format("REST shareFile: filename = %s, userId = %s, userIdShare = %s, password =%s\n", filename,
                userId, userIdShare, password));

        super.resultOrThrow(impl.shareFile(filename, userId, userIdShare, password));

        String[] operationInfo = {filename, userId, userIdShare};

        toe.publish(OP_SHARE, JSON.encode(operationInfo));
    }

    @Override
    public void unshareFile(String filename, String userId, String userIdShare, String password) {
        Log.info(String.format("REST unshareFile: filename = %s, userId = %s, userIdShare = %s, password =%s\n",
                filename, userId, userIdShare, password));

        super.resultOrThrow(impl.unshareFile(filename, userId, userIdShare, password));

        String[] operationInfo = {filename, userId, userIdShare};

        toe.publish(OP_UNSHARE, JSON.encode(operationInfo));
    }

    @Override
    public byte[] getFile(String filename, String userId, String accUserId, String password) {
        Log.info(String.format("REST getFile: filename = %s, userId = %s, accUserId = %s, password =%s\n", filename,
                userId, accUserId, password));

        var res = impl.getFile(filename, userId, accUserId, password);
        if (res.error() == Result.ErrorCode.REDIRECT) {
            String location = res.errorValue();
            if (!location.contains(REST))
                res = FilesClients.get(location).getFile(JavaDirectory.fileId(filename, userId), password);
        }

        return super.resultOrThrow(res);
    }

    @Override
    public List<FileInfo> lsFile(String userId, String password) {
        long T0 = System.currentTimeMillis();
        try {

            Log.info(String.format("REST lsFile: userId = %s, password = %s\n", userId, password));

            return super.resultOrThrow(impl.lsFile(userId, password));
        } finally {
            System.err.println("TOOK:" + (System.currentTimeMillis() - T0));
        }
    }

    @Override
    public void deleteUserFiles(String userId, String password, String token) {
        Log.info(
                String.format("REST deleteUserFiles: user = %s, password = %s, token = %s\n", userId, password, token));

        super.resultOrThrow(impl.deleteUserFiles(userId, password, token));
    }
}
