/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioSystem;
import static javax.sound.sampled.AudioSystem.getMixer;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class App {

    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException {
        System.out.println("Starting up...");
        File IF = new File("./testaudio/tugip_mfp.wav");
        int IFS = (int) IF.length();
        AudioInputStream AIS = AudioSystem.getAudioInputStream(IF);
        AudioFormat AF = AIS.getFormat();

        double AL = ((1000.0*IFS) / (AF.getFrameSize() * AF.getFrameRate()));
        byte[] Buffer = new byte[IFS];
        AIS.read(Buffer);
        System.out.println("Red "+Buffer.length +" bytes from " + IF.toString()+", should be "+AL+" miliseconds long.");
        
        DataLine.Info DLI = new DataLine.Info(SourceDataLine.class, AF);

        Mixer.Info[] Ms = AudioSystem.getMixerInfo();
        for (Mixer.Info M : Ms) {
            Mixer CM = getMixer(M);
//            if (M==Ms[1]) {continue;}
            Line.Info[] Ls = CM.getSourceLineInfo(DLI);
            if (Ls.length > 0) {
                System.out.println("Mixer " + M.toString() + " has " + Ls.length + " lines for " + DLI.toString() + ".");
            }

            for (Line.Info CLI : Ls) {
                try {
                    SourceDataLine CL = (SourceDataLine) CM.getLine(CLI);
                    System.out.println("Playing on line: " + CL.getLineInfo().toString());
                    CL.open(AF, (int) IF.length());
                    CL.write(Buffer, 0, Buffer.length);
                    
                    for (int C=0; C*33<AL ; C++) {
//                        System.out.println("C/Level:\t"+C+"\t"+CL.getLevel()+" while "+AudioSystem.NOT_SPECIFIED); System.out.flush();
                        try {
                            Thread.sleep(33);
                        } catch (InterruptedException IE) {
                            System.out.println("Interrupted.");
                        }
                        
                    }
                    CL.close();
                    System.out.println("Finished playing.");
                } catch (LineUnavailableException LUE) {
                    System.err.println("LineUnavailableException: "+LUE); // LUE.printStackTrace();

                }
            }
        }
        System.out.flush();
        System.out.println("Exiting...");
    }
}
