/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JOptionPane;

public class App {
    public static final String VERSION="0.6.14";
    
    public static final String[] SYSTEM_SOUND_DIRS = new String[]{"systemletters/", "systemnumbers/", "systemsounds/"};

    public static Lecturer L;
    public final static Sounder S = new Sounder(SYSTEM_SOUND_DIRS[2] + "hello.wav");
    public final static GUI G = new GUI(S);

    public static void redAlert(String message) {
        JOptionPane.showMessageDialog(G, message, message, JOptionPane.ERROR_MESSAGE);
        System.exit(-1);
    }

    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException, LineUnavailableException {
        System.out.println("Starting up, Sounder and GUI already static initialized.");

        ArrayList<String> lectureNames = Lecturer.listAvailableLecturesSorted();
        String myLecture = Lecturer.progressProperties.getProperty("nextLecture");
        
//shred        System.err.println("DEBUG: myLecture: "+myLecture+" and lectureNames: "+lectureNames.toString());
               
        if ((null == lectureNames) || (lectureNames.isEmpty())) {
            redAlert("No lectures: " + lectureNames);
        }

        L = new Lecturer(lectureNames.contains(myLecture) ? myLecture : lectureNames.get(0));

        G.startLecture(false);
    }
}
