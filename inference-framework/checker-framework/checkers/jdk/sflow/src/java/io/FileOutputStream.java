package java.io;
import checkers.inference.reim.quals.*;
import checkers.inference.sflow.quals.*;

import java.nio.channels.FileChannel;

public class FileOutputStream extends OutputStream {
    public FileOutputStream(/*@Safe*/ String name) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileOutputStream(/*@Safe*/ String name, boolean append) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileOutputStream(FileDescriptor fdObj) {
        throw new RuntimeException("skeleton method");
    }

    public native void write(int b) throws IOException;

    public void write(byte b @Readonly []) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void write(byte b @Readonly [], int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final FileDescriptor getFD(@Readonly FileOutputStream this)  throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public FileChannel getChannel() {
        throw new RuntimeException("skeleton method");
    }

    protected void finalize() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
