package tp1.impl.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import tp1.api.service.java.DirectoryRep;
import tp1.impl.kafka.sync.SyncPoint;
import tp1.impl.servers.common.JavaDirectory;
import tp1.impl.servers.common.JavaRepDirectory;
import tp1.impl.servers.rest.DirectoryRepResources;
import util.JSON;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

public class RecordProcessorClass implements RecordProcessor{

    private static final String OP_WRITE = "write";
    private static final String OP_DELETE = "delete";
    private static final String OP_SHARE = "share";
    private static final String OP_UNSHARE = "unshare";

    private final JavaRepDirectory repDir;

    public RecordProcessorClass(JavaRepDirectory repDir){
        this.repDir = repDir;
    }


    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
        KafkaEvent info = JSON.decode(r.value(), KafkaEvent.class);

        String filename = info.getFilename();
        String userId = info.getUserID();

        String recordKey = r.key();

        switch(recordKey){
            case(OP_WRITE):

                JavaDirectory.ExtendedFileInfo fileInfo = info.getFile();
                SyncPoint.getInstance().setResult(r.offset(), repDir.writeFileEvent(filename,userId, fileInfo));
                break;
            case(OP_DELETE):
                SyncPoint.getInstance().setResult(r.offset(), repDir.deleteFileEvent(filename,userId));
                break;
            case(OP_SHARE):
                String userIdShare = info.getUserShareId();
                SyncPoint.getInstance().setResult(r.offset(), repDir.shareFileEvent(filename,userId, userIdShare));
                break;
            case(OP_UNSHARE):
                String userIdUnshare = info.getUserShareId();
                SyncPoint.getInstance().setResult(r.offset(), repDir.unshareFile(filename,userId, userIdUnshare));
                break;
        }
    }
}
