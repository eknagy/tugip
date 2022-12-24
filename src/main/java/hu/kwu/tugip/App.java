/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.IOException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JOptionPane;

public class App {

    public final static String SYSTEMSOUNDDIR="systemsounds/";
    public final static String NUMBERSSOUNDDIR="numbers/";
    public final static Sounder S = new Sounder(SYSTEMSOUNDDIR+"hello.wav");
    public final static GUI SingletonGUI = new GUI(S);
    public static Lecturer L;

    public static void alertRed(String message) {
        new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
        System.exit(-1);
    }
    
    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException, LineUnavailableException {
        System.out.println("Starting up, Sounder and GUI already static initialized.");
        
        String [] lectureNames = L.listAvailableLecturesSorted();
        
        if ((null==lectureNames) || (lectureNames.length==0)) {
            alertRed("No lectures: "+lectureNames);
        }
        
        L = new Lecturer(lectureNames[0]);

        S.syncPlayOnSelectedLine("systemsounds/hello.wav");

        String [] HelloFileNames = L.getHelloFilesNames();
//        System.out.println("Debug HelloFileNames: "+Arrays.toString(HelloFileNames));
        if (null!= HelloFileNames) {
            SingletonGUI.startLecture(HelloFileNames);
        } else {
            throw new RuntimeException("UNIMPLEMENTED! BEFORE_LECTURE is parsed to null or not single-item String array.");
        }
    }
}
