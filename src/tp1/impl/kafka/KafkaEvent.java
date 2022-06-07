package tp1.impl.kafka;

import tp1.impl.servers.common.JavaDirectory;

public class KafkaEvent {

    final private String filename;
    final private String userID;
    final private String userShareId;
    final private JavaDirectory.ExtendedFileInfo file;

    public KafkaEvent(String filename, String userID, String userShareId, JavaDirectory.ExtendedFileInfo file){
        this.filename = filename;
        this.userID = userID;
        this.userShareId = userShareId;
        this.file = file;
    }

    public String getFilename(){
        return filename;
    }

    public String getUserID(){
        return userID;
    }

    public String getUserShareId(){
        return userShareId;
    }

    public JavaDirectory.ExtendedFileInfo getFile(){
        return file;
    }
}
