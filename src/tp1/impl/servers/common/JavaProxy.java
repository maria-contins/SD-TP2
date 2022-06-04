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
import tp1.impl.servers.rest.proxyMsgs.CreateFolderV2Args;
import tp1.impl.servers.rest.proxyMsgs.DeleteFolderOrFileArgs;
import tp1.impl.servers.rest.proxyMsgs.GetFile;
import tp1.impl.servers.rest.proxyMsgs.UploadArgs;
import util.Sleep;


public class JavaProxy implements Files {

    //Dropbox API stuff
    private static final String apiKey = "hco8hk1gqbs4lle";
    private static final String apiSecret = "eorx05nr0ghxefn";
    private static final String accessTokenStr = "sl.BI7wNZevyRJhwMWphylKy04nQct3CvHdlk6lF3_ipv4weZieJVBq0qZo--YAsSMmH2FiAtRXx96ReLQ5B_lHBG9OgqpwMc4KaGR5eayx-RJqxqEXhAPyi-F2QtCCxVj2vc_6BQBADIHO";
    //API Request Links
    private static final String DELETE_FOLDER_OR_FILE_URL = "https://api.dropboxapi.com/2/files/delete_v2";
    private static final String GET_FILE_URL = "https://content.dropboxapi.com/2/files/download";
    private static final String UPLOAD_FILE_URL = "https://content.dropboxapi.com/2/files/upload";
    private static final String CREATE_FOLDER_V2_URL = "https://api.dropboxapi.com/2/files/create_folder_v2";

    public static final String DELIMITER = "\\$\\$\\$";
    public static final String ROOT_MAIN = "/Main/";
    public static final String ROOT_FILES = "/Main/Files/";
    public static final String ROOT_USERS = "/Main/UserFiles/";

    private final Gson json;
    private final OAuth20Service service;
    private final OAuth2AccessToken accessToken;

    private static final int HTTP_SUCCESS = 200;

    private static final String CONTENT_TYPE_HDR = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json";
    protected static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
    private static final int RETRIES = 3;

    public JavaProxy() {
        super();
        json = new Gson();
        accessToken = new OAuth2AccessToken(accessTokenStr);
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
    }

    @Override
    public Result<byte[]> getFile(String fileId, String token) {
        OAuthRequest getFile = new OAuthRequest(Verb.POST, GET_FILE_URL);
        getFile.addHeader("Content-Type", OCTET_STREAM_CONTENT_TYPE);
        getFile.addHeader("Dropbox-API-Arg", json.toJson(new GetFile(ROOT_FILES + fileId)));

        service.signRequest(accessToken, getFile);
        int retry = 0;

        Response r = null;
        while (retry < RETRIES && r == null) {
            retry++;
            try {
                r = service.execute(getFile);
                if (r.getCode() != HTTP_SUCCESS)
                    throw new RuntimeException(String.format("Failed to get File: %s, Status: %d, \nReason: %s\n",
                            r.getCode(), r.getBody()));
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

    @Override
    public Result<Void> deleteFile(String fileId, String token) {
        String[] tokens = fileId.split(DELIMITER);
        String userId = tokens[0];

        delete(ROOT_FILES + fileId);
        delete(ROOT_USERS + userId + "/" + fileId);

        return Result.ok();
    }

    @Override
    public Result<Void> writeFile(String fileId, byte[] data, String token) {
        String[] tokens = fileId.split(DELIMITER);
        String userId = tokens[0];

        createFolder("/Main/UserFiles/" + userId);

        upload("/Main/Files/" + fileId, data);
        upload("/Main/UserFiles/" + userId + "/" + fileId, data);

        return Result.ok();
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String token) { // TODO
        delete(ROOT_USERS + userId);

        return Result.ok();
    }

    public boolean createFolder(String path) {
        var createFolder = new OAuthRequest(Verb.POST, CREATE_FOLDER_V2_URL);
        createFolder.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);

        createFolder.setPayload(json.toJson(new CreateFolderV2Args(path, false)));

        service.signRequest(accessToken, createFolder);

        int retry = 0;
        while (retry < RETRIES) {
            try {
                Response r = service.execute(createFolder);
                // 409 means folder already exists
                if (r.getCode() == 200 || r.getCode() == 409)
                    return true;
                if (r.getCode() == 429) {
                    retry++;
                    Thread.sleep(Integer.parseInt(r.getHeader("Retry-After")));
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                retry++;
            }
        }
        return false;
    }

    public void upload(String path, byte[] data) {
        OAuthRequest writeFile = new OAuthRequest(Verb.POST, UPLOAD_FILE_URL);
        writeFile.addHeader("Content-Type", OCTET_STREAM_CONTENT_TYPE);
        writeFile.addHeader("Dropbox-API-Arg", json.toJson(new UploadArgs(path, "overwrite")));

        writeFile.setPayload(data);
        service.signRequest(accessToken, writeFile);

        int retry = 0;

        Response r = null;
        while (retry < RETRIES && r == null) {
            retry++;
            try {
                r = service.execute(writeFile);
                System.out.println(r.getCode());
                System.out.println(r.getBody());
                if (r.getCode() == 200)
                    break;
                else if (r.getCode() == 429) {
                    retry++;
                    Sleep.seconds(3);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void delete(String path) {
        OAuthRequest delete = new OAuthRequest(Verb.POST, DELETE_FOLDER_OR_FILE_URL);
        delete.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);
        delete.setPayload(json.toJson(new DeleteFolderOrFileArgs(path)));

        service.signRequest(accessToken, delete);
        int retry = 0;

        Response r = null;
        while (retry < RETRIES && r == null) {
            retry++;
            try {
                r = service.execute(delete);
                if (r.getCode() == 200)
                    break;
                else if (r.getCode() == 429) {
                    retry++;
                    Sleep.seconds(3);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}