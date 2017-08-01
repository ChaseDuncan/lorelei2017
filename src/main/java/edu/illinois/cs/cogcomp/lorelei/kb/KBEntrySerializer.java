package edu.illinois.cs.cogcomp.lorelei.kb;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializer;

import java.io.IOException;

/**
 * Created by mayhew2 on 7/28/17.
 */
public class KBEntrySerializer implements Serializer<KBEntry> {
    @Override
    public void serialize(@NotNull DataOutput2 dataOutput2, @NotNull KBEntry kbEntry) throws IOException {
        dataOutput2.write(kbEntry.id);
        dataOutput2.writeBytes(kbEntry.asciiname);
    }

    @Override
    public KBEntry deserialize(@NotNull DataInput2 dataInput2, int i) throws IOException {
        int id = dataInput2.readInt();
        String asciiname = dataInput2.readUTF();
        return new KBEntry(id, asciiname);
    }
}
