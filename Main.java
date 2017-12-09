import java.io.File;
import java.io.IOException;
import java.util.BitSet;

/**
 * Created by Almaz on 02.11.2017.
 */
public class Main {

    public static void main(String[] args) throws IOException {

        LZ78 lz78 = new LZ78();
        lz78.compress(new File("textFile.txt"));
        lz78.decompress(new File("compressed_textFile.txt"));
    }
}
