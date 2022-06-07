package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.api.service.java.Files;
import tp1.impl.servers.common.JavaProxy;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import util.Debug;
import util.Token;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxyServer extends AbstractRestServer{

    public static final int PORT = 27015;

    private static Logger Log = Logger.getLogger(ProxyServer.class.getName());


    ProxyServer() {
        super(Log, Files.SERVICE_NAME, PORT);
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register( ProxyResources.class );
        config.register( GenericExceptionMapper.class );
//        config.register( CustomLoggingFilter.class);
    }

    public static void main(String[] args) throws Exception {

        Debug.setLogLevel( Level.INFO, Debug.TP1);

        Token.set( args.length == 0 ? "" : args[1] );

        String flag = args[0];
        JavaProxy dropbox = new JavaProxy();

        if(flag.equals("true")){
            dropbox.delete("/Main");
            boolean createMainFolder = dropbox.createFolder("/Main");
            dropbox.createFolder("/Main/Files");
            dropbox.createFolder("/Main/UserFiles");
            if (createMainFolder)
                System.out.println("Directory created successfuly.");
            else
                System.out.println("Failed to create directory");
        }
        new ProxyServer().start();
    }
}