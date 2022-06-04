package tp1.impl.servers.common;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.api.service.java.Files;
import tp1.api.service.java.Result;
import tp1.impl.servers.rest.proxyMsgs.DeleteArgs;
import tp1.impl.servers.rest.proxyMsgs.UploadArgs;
import util.Sleep;

import java.util.Collections;


public class JavaProxy{

    //Dropbox API stuff
    private static final String apiKey = "hco8hk1gqbs4lle";
    private static final String apiSecret = "eorx05nr0ghxefn";
    private static final String accessTokenStr = "sl.BI0Xz3CvdOuy8sXThk85hbs1ejWGhBwrMverhNNL35C7oThG3aXdHECgCjT7l52o0v3WnSAWIlf6I0Tw1i6ce0k2c7zr0gcyiZ4J1nzniBI4QMEE1rCRuTJn-xwfpI870Knx1wgLNcsV";

    //API Request Links
    private static final String DELETE_FOLDER_OR_FILE_URL = "https://api.dropboxapi.com/2/files/delete_v2";
    private static final String GET_FILE_URL = "https://content.dropboxapi.com/2/files/download";
    private static final String UPLOAD_FILE_URL = "https://content.dropboxapi.com/2/files/upload";

    private final Gson json;
    private final OAuth20Service service;
    private final OAuth2AccessToken accessToken;

    private static final int HTTP_SUCCESS = 200;

    private static final String CONTENT_TYPE_HDR = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json";
    protected static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
    private static final int RETRIES = 3;

    public JavaProxy(){
        super();
        json = new Gson();
        accessToken = new OAuth2AccessToken(accessTokenStr);
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
    }

    public Result<byte[]> getFile(String fileId, String token) {
        OAuthRequest getFile = new OAuthRequest(Verb.POST, GET_FILE_URL);
        getFile.addHeader("Content-Type", OCTET_STREAM_CONTENT_TYPE);
        getFile.addHeader("Dropbox-API-Arg", json.toJson("/" + fileId));

        service.signRequest(accessToken, getFile);
        int retry = 0;

        Response r = null;
        while (retry < RETRIES && r==null) {
            retry++;
            try {
                var response = service.execute(getFile);
                if (response.getCode() != HTTP_SUCCESS)
                    throw new RuntimeException(String.format("Failed to create directory: %s, Status: %d, \nReason: %s\n",
                            directoryName, response.getCode(), response.getBody()));
                if (r.getCode() == 200)
                    return Result.ok(r.getStream().readAllBytes());
                else if (r.getCode() == 429) {
                    retry++;
                    Sleep.seconds(3);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Result<Void> deleteFile(String fileId, String token) {
        OAuthRequest delete = new OAuthRequest(Verb.POST, DELETE_FOLDER_OR_FILE_URL);

        delete.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        delete.setPayload(json.toJson(new DeleteArgs(Collections.singletonList(fileId))));

        service.signRequest(accessToken, delete);
       int retry = 0;

       Response r = null;
       while (retry < RETRIES && r==null) {
           retry++;
           try {
                r =service.execute(delete);
                if (r.getCode() == 200)
                    return Result.ok();
                else if (r.getCode() == 429) {
                    retry++;
                    Sleep.seconds(3);
                }
           } catch (Exception e) {
               e.printStackTrace();
           }
       }
        return null;
    }

    public Result<Void> writeFile(String fileId, byte[] data, String token) {
        OAuthRequest writeFile = new OAuthRequest(Verb.POST, UPLOAD_FILE_URL);
        writeFile.addHeader("Content-Type", OCTET_STREAM_CONTENT_TYPE);
        writeFile.addHeader("Dropbox-API-Arg", json.toJson(new UploadArgs("/" + fileId, false)));

        service.signRequest(accessToken, writeFile);
        int retry = 0;

        Response r = null;
        while (retry < RETRIES && r==null) {
            retry++;
            try {
                r =service.execute(writeFile);
                System.out.println(r.getCode());
                System.out.println(r.getBody());
                if (r.getCode() == 200)
                    return Result.ok();
                else if (r.getCode() == 429) {
                    retry++;
                    Sleep.seconds(3);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public Result<Void> deleteUserFiles(String userId, String token) {
        return null;
    }
}