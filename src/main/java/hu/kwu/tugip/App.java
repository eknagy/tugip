/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.IOException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;

public class App {

    public final static Sounder S = new Sounder("systemsounds/tugip_hello.wav");
    public final static GUI SingletonGUI = new GUI();

    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException, LineUnavailableException {
        System.out.println("Starting up, Sounder and GUI already static initailized.");
        Lecturer L = new Lecturer("test_keys");
        System.out.println("Loaded Lecturer.");
//        S.playOnSelectedLine(L.getHelloFileName());
        S.playOnSelectedLine("systemsounds/tugip_hello.wav");
    }
}
