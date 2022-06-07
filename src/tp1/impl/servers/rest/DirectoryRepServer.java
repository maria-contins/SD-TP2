package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.api.service.java.Directory;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import util.Debug;
import util.Token;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryRepServer extends AbstractRestServer {
    public static final int PORT = 5109;

    private static Logger Log = Logger.getLogger(DirectoryRepServer.class.getName());

    DirectoryRepServer() {
        super(Log, Directory.SERVICE_NAME, PORT);
        //SyncPoint.getInstance();
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register( DirectoryRepResources.class );
        config.register( GenericExceptionMapper.class );
//		config.register( CustomLoggingFilter.class);
    }

    public static void main(String[] args) throws Exception {

        Debug.setLogLevel( Level.INFO, Debug.TP1);

        Token.set( args.length > 0 ? args[0] : "");

        new DirectoryRepServer().start();
    }
}
