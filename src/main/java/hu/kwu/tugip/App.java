/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.IOException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;

public class App {

    final static Sounder S = new Sounder("systemsounds/tugip_mfp.wav");
    final static GUI MyGUI = new GUI();

    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException, LineUnavailableException {
        System.out.println("Starting up, Sounder and GUI already static initailized.");
        Lecturer L = new Lecturer("test_keys");
        System.out.println("Loaded Lecturer.");
        S.playOnSelectedLine(L.getHelloFileName(), MyGUI);
        System.out.println("Exiting...");
    }
}
