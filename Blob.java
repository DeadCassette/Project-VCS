package gitlet;
import java.io.File;
import java.io.Serializable;

/** Each blob is a unique snapshot of a file from a specific time.
 * @author Morgan Sinnock & Conrad Ehlers */

public class Blob implements Serializable {
    /** holds the contents of the blob's associated file */
    private byte[] contents; //contents of file is a
    /** the file's name */
    private String fileName;

    private String contentsAsString;

    /** Blob constructor reads the contents of a file */
    public Blob(String fileName) {
        // reading our File as a string, then serializing it into a sha1 id byte array
        this.contents = Utils.serialize(Utils.readContentsAsString(new File(fileName))); // check if works
        // getName() returns the last name in the pathname's name sequence */
        this.fileName = fileName;

        this.contentsAsString = Utils.readContentsAsString(new File(fileName));
    }

    public String getSha1() {
        byte[] serializedBlob = Utils.serialize(this);
        // hashing an extra word to distinguish blob id from commit id; same in getter for Commit class
        return Utils.sha1(serializedBlob, "blob");
    }

    /** getter for Blob's contents */
    public byte[] getContents() {
        return contents;
    }

    public String getContentsAsString() {
        return contentsAsString;
    }

    /** getter for fileName this Blob represents */
    public String getFileName() {
        return this.fileName;
    }
}
