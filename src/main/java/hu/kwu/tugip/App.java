/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    static int VisualSampleConstant=-1; // To provide a visual volume feedback at 30 fps, we need an even (2-byte sample size) index increment
    static boolean AutoVolumeEnabled=true;
    static ByteBuffer BB;

    static AudioFormat AF; // Should be file-specific
    static int IFL; // Should be file-specific
    
    static {
        BB= ByteBuffer.allocate(2); // We need not to have to fight with binary complements in 2-byte <=> short conversions
        BB.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    public static byte [] loadWavToBuffer(String FileName) throws UnsupportedAudioFileException, IOException{
        File IF = new File(FileName);
        IFL = (int) IF.length();
        AudioInputStream AIS = AudioSystem.getAudioInputStream(IF);
        AF = AIS.getFormat();
        double ALims = ((1000.0 * IFL) / (AF.getFrameSize() * AF.getFrameRate())); //Audio Length in miliseconds
        byte[] Buffer = new byte[IFL];
        AIS.read(Buffer);
        System.out.println("Red " + Buffer.length + " bytes from " + IF.toString() + ", should be " + ALims + " miliseconds long, framesize is " + AF.getFrameSize());

        int MyVisualSampleConstant = (int) (AF.getFrameRate() * AF.getFrameSize() / 30);
        if (MyVisualSampleConstant % 2 == 1) {
            MyVisualSampleConstant--;
        }
        if (VisualSampleConstant==-1) {
            VisualSampleConstant=MyVisualSampleConstant;
        } else if (VisualSampleConstant != MyVisualSampleConstant) {
            throw new UnsupportedAudioFileException("VisualSampleConstant (bitrate) mismatch!");
        }

        if (AutoVolumeEnabled) {
            // We need to get the minimum and the maximum so we can see if we can turn on auto-volume-gain and to determine the gain ratio
            int Min = Integer.MAX_VALUE;
            int Max = Integer.MIN_VALUE;
            short Tmp;
        
            for (int i = 0; i < Buffer.length; i += 2) {
                BB.put(0, Buffer[i]);
                BB.put(1, Buffer[i + 1]);
                Tmp = BB.getShort(0);
                if (Tmp < Min) {
                    Min = Tmp;
                }
                if (Tmp > Max) {
                    Max = Tmp;
                }
            }

            System.out.println("Value min and max: " + Min + " " + Max);

            if ((Min < -1024) && (Max > 1024)) {
                int Absolut = ((Min * -1) > Max) ? (Min * -1) : Max;
                short Ratio = (short) (Short.MAX_VALUE / Absolut);
                if (Ratio > 1) {
                    System.out.println("AutoVolume constant is: " + Ratio);
                    for (int i = 0; i < Buffer.length; i += 2) {
                        BB.order(ByteOrder.LITTLE_ENDIAN);
                        BB.put(0, Buffer[i]);
                        BB.put(1, Buffer[i + 1]);
                        Tmp = BB.getShort(0);
                        BB.putShort(0, (short) (Tmp * Ratio));
                        Buffer[i] = BB.get(0);
                        Buffer[i + 1] = BB.get(1);

                    /*                    if (i % (VisualSampleConstant * 10) == 0) {
                        System.err.println(i + ": " + Tmp + ": " + Buffer[i] + " " + Buffer[i + 1]);
                    }
                     */
                    }
                }
            }
        }

        return Buffer;
    }
    
    

    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException {
        System.out.println("Starting up...");
        GUI MyGUI=new GUI();
//        File IF = new File("./testaudio/nosound.wav");
//        File IF = new File("./testaudio/csend.wav");
//        File IF = new File("./testaudio/hallo.wav");
        byte[] Buffer = loadWavToBuffer("./testaudio/tugip_mfp.wav");

        DataLine.Info DLI = new DataLine.Info(SourceDataLine.class, AF);

        Mixer.Info[] Ms = AudioSystem.getMixerInfo();
        for (Mixer.Info M : Ms) {
            Mixer CM = getMixer(M);
//            if (M==Ms[1]) {continue;}
            Line.Info[] Ls = CM.getSourceLineInfo(DLI);
            if (Ls.length > 0) {
                System.out.println("Mixer " + M.toString() + " has " + Ls.length + " lines for " + DLI.toString() + ".");
            }
            MyGUI.setIntensity(255, false);
                   
            for (Line.Info CLI : Ls) {
                try {
                    SourceDataLine CL = (SourceDataLine) CM.getLine(CLI);
                    System.out.println("Playing on line: " + CL.getLineInfo().toString());
                    CL.open(AF, (int) IFL);
                    CL.write(Buffer, 0, Buffer.length);

                    for (int C = 0; C * VisualSampleConstant < Buffer.length; C++) {
                        BB.put(0, Buffer[C * VisualSampleConstant]);
                        BB.put(1, Buffer[C * VisualSampleConstant + 1]);
                        short Tmp = BB.getShort(0);
                        int VolumeLevel=Math.min(255, Math.abs(Tmp)/32);
//                        System.out.println("DEBUG: " +Tmp+" "+Math.abs(Tmp)/32+" "+VolumeLevel);
                        MyGUI.setIntensity(VolumeLevel, true);

                        try {
                            Thread.sleep(33);
                        } catch (InterruptedException IE) {
                            System.out.println("Interrupted.");
                        }
                    }
                    CL.close();
                    System.out.println("Finished playing.");
                    MyGUI.close();
                } catch (LineUnavailableException LUE) {
                    System.err.println("LineUnavailableException: " + LUE); // LUE.printStackTrace();

                }
            }
            MyGUI.setIntensity(255, false);

        }
        System.out.flush();
        System.out.println("Exiting...");
    }
}
