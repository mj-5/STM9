package id.stsn.stm9.utility;

import java.io.IOException;
import java.io.InputStream;

public class PositionAwareInputStream extends InputStream {
    private InputStream mStream;
    private long mPosition;

    public PositionAwareInputStream(InputStream in) {
        mStream = in;
        mPosition = 0;
    }

    @Override
    public int read() throws IOException {
        int ch = mStream.read();
        ++mPosition;
        return ch;
    }
    
    public long position() {
        return mPosition;
    }
}
