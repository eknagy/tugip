/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;

public class App {

    public final static Sounder S = new Sounder("systemsounds/hello.wav");
    public final static GUI SingletonGUI = new GUI(S);
    public static Lecturer L;
    
    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException, LineUnavailableException {
        System.out.println("Starting up, Sounder and GUI already static initialized.");
        L = new Lecturer("test_keys");
        System.out.println("Loaded Lecturer.");
        S.syncPlayOnSelectedLine("systemsounds/hello.wav");

        String [] HelloFileNames = L.getHelloFilesNames();
        System.out.println("Debug HelloFileNames: "+Arrays.toString(HelloFileNames));
        if ((null!= HelloFileNames) && (HelloFileNames.length==1)) {
            SingletonGUI.startLecture(HelloFileNames);
        } else {
            throw new RuntimeException("UNIMPLEMENTED! BEFORE_LECTURE is parsed to null or not single-item String array.");
        }
    }
}
