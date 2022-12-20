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

    public final static Sounder S = new Sounder("systemsounds/tugip_hello.wav");
    public final static GUI SingletonGUI = new GUI();

    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException, LineUnavailableException {
        System.out.println("Starting up, Sounder and GUI already static initailized.");
        Lecturer L = new Lecturer("test_keys");
        System.out.println("Loaded Lecturer.");
        String [] HelloFileNames = L.getHelloFilesNames();
        if ((null!= HelloFileNames) && (HelloFileNames.length>0)) {
            System.out.println("Debug X: "+Arrays.toString(HelloFileNames));

            for (String CS : HelloFileNames) {
                S.syncPlayOnSelectedLine(CS);
                System.out.println("Playing "+CS);
            }
            System.out.println("Finished ;)");
            App.SingletonGUI.close();
        } else {
            throw new RuntimeException("UNIMPLEMENTED! BEFORE_LECTURE is parsed to null or String[0].");
        }
//        S.playOnSelectedLine("systemsounds/tugip_hello.wav");
    }
}
