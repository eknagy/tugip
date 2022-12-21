/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.IOException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;

public class App {

    public final static Sounder S = new Sounder("systemsounds/hello.wav");
    public final static GUI SingletonGUI = new GUI(S);
    public static Lecturer L;
    
    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException, LineUnavailableException {
        System.out.println("Starting up, Sounder and GUI already static initailized.");
        L = new Lecturer("test_keys");
        System.out.println("Loaded Lecturer.");
        S.syncPlayOnSelectedLine("systemsounds/hello.wav");

        String [] HelloFileNames = L.getHelloFilesNames();
        if ((null!= HelloFileNames) && (HelloFileNames.length>0)) {
//            System.out.println("Debug X: "+Arrays.toString(HelloFileNames));

            for (String CS : HelloFileNames) {
                S.syncPlayOnSelectedLine(CS);
            }
            SingletonGUI.startLecture();
        } else {
            throw new RuntimeException("UNIMPLEMENTED! BEFORE_LECTURE is parsed to null or String[0].");
        }
    }
}
