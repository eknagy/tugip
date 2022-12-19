/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Lecturer {
    private final Properties P = new Properties();
    private final String LectureName;

    public Lecturer(String LectureName) throws FileNotFoundException, IOException {
        this.LectureName = LectureName;
        BufferedInputStream FIS = new BufferedInputStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("lectures/" + this.LectureName + "/config.properties"));
        P.load(FIS);
    }

    public String getHelloFileName() throws IOException{
/*        String HelloPathString = "lectures/" + this.LectureName + "/" + P.getProperty("BEFORE_LECTURE") + ".wav";
        Path HelloPath = Paths.get(HelloPathString);
        if ((!Files.exists(HelloPath)) || (!Files.isReadable(HelloPath)) || (!Files.isRegularFile(HelloPath))) {
              throw new IOException("Not {found, readable, a file}: "+HelloPath.toString()+" ("+HelloPathString+")");
        }
*/
        return ("lectures/" + this.LectureName + "/" + P.getProperty("BEFORE_LECTURE") + ".wav");
    }
}
