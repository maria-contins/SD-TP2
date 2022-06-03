package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.api.service.java.Files;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import util.Debug;
import util.Token;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FilesProxyServer extends AbstractRestServer{

    public static final int PORT = 27015;

    private static Logger Log = Logger.getLogger(FilesProxyServer.class.getName());


    FilesProxyServer() {
        super(Log, Files.SERVICE_NAME, PORT);
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register( FilesResources.class );
        config.register( GenericExceptionMapper.class );
//		config.register( CustomLoggingFilter.class);
    }

    public static void main(String[] args) throws Exception {

        Debug.setLogLevel( Level.INFO, Debug.TP1);

        Token.set( args.length == 0 ? "" : args[0] );

        new FilesProxyServer().start();
    }
}
