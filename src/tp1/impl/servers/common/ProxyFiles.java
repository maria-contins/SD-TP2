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
import tp1.impl.servers.rest.proxyMsgs.DeleteFileOrFolderArgs;

public class ProxyFiles implements Files{

    //Dropbox API stuff
    private static final String apiKey = "hco8hk1gqbs4lle";
    private static final String apiSecret = "eorx05nr0ghxefn";
    private static final String accessTokenStr = "sl.BG0fhzHHN3lMsttmcfN-j5LEwmjUUVsVbGfcEmNvHno5p3F8U4tkireS7obpW3a6H9zRFjFGG5Y6pUdokhcPtKauPdlUeZrRczav14RXyK6-FP3iqfObr-Z-Hu_tbS2Epd8h98anwgqE";

    //API Request Links
    private static final String CREATE_FILE = "https://api.dropboxapi.com/2/file_requests/create";
    private static final String CREATE_FOLDER_V2_URL = "https://api.dropboxapi.com/2/files/create_folder_v2";
    private static final String DELETE_FOLDER_OR_FILE_V2 = "https://api.dropboxapi.com/2/files/delete_v2";

    private final Gson json;
    private final OAuth20Service service;
    private final OAuth2AccessToken accessToken;

    private static final int HTTP_SUCCESS = 200;
    private static final String CONTENT_TYPE_HDR = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json";

    public ProxyFiles(){
        super();
        json = new Gson();
        accessToken = new OAuth2AccessToken(accessTokenStr);
        service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
    }

    @Override
    public Result<byte[]> getFile(String fileId, String token) {
        return null;
    }

    @Override
    public Result<Void> deleteFile(String fileId, String token) {
        try{
            var delete = new OAuthRequest(Verb.POST, DELETE_FOLDER_OR_FILE_V2);
            delete.addHeader(CONTENT_TYPE_HDR, JSON_CONTENT_TYPE);

            delete.setPayload(json.toJson(new DeleteFileOrFolderArgs(fileId)));

            service.signRequest(accessToken, delete);

            Response r = service.execute(delete);

            return r;
        } catch (Exception e){
            System.out.print("ERRO"); //TODO
        }

        return null;
    }

    @Override
    public Result<Void> writeFile(String fileId, byte[] data, String token) {


        return null;
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String token) {
        return null;
    }
}
