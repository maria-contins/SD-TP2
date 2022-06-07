package tp1.impl.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import tp1.api.FileInfo;
import tp1.api.service.java.DirectoryRep;
import tp1.impl.kafka.sync.SyncPoint;
import tp1.impl.servers.common.JavaDirectory;
import tp1.impl.servers.common.JavaRepDirectory;
import tp1.impl.servers.rest.DirectoryRepResources;
import util.JSON;

import java.io.File;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Logger;

public class RecordProcessorClass implements RecordProcessor{

    private static final String OP_WRITE = "write";
    private static final String OP_DELETE = "delete";
    private static final String OP_SHARE = "share";
    private static final String OP_UNSHARE = "unshare";

    private final JavaRepDirectory repDir;

    final static Logger Log = Logger.getLogger(RecordProcessorClass.class.getName());

    public RecordProcessorClass(JavaRepDirectory repDir){
        this.repDir = repDir;
    }


    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
        Map<String, String> eventInfo = JSON.decode(r.value());
        String filename = eventInfo.get("filename");
        String userId = eventInfo.get("userId");

        String recordKey = r.key();

        switch(recordKey){
            case(OP_WRITE):
                String fileId = eventInfo.get("fileId");
                URI[] uris = JSON.decode(eventInfo.get("uris"), URI[].class);
                FileInfo info = JSON.decode(eventInfo.get("info"),FileInfo.class);

                var file = new JavaDirectory.ExtendedFileInfo(List.of(uris), fileId, info);

                SyncPoint.getInstance().setResult(r.offset(), repDir.writeFileEvent(filename,userId, file));
                break;
            case(OP_DELETE):
                SyncPoint.getInstance().setResult(r.offset(), repDir.deleteFileEvent(filename,userId));
                break;
            case(OP_SHARE):
                //String userIdShare = info.getUserShareId();
                //SyncPoint.getInstance().setResult(r.offset(), repDir.shareFileEvent(filename,userId, userIdShare));
                break;
            case(OP_UNSHARE):
                //String userIdUnshare = info.getUserShareId();
                //SyncPoint.getInstance().setResult(r.offset(), repDir.unshareFile(filename,userId, userIdUnshare));
                break;
        }
    }
}
