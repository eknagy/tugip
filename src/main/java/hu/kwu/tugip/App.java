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

    public static void main(String[] args) throws RuntimeException, ClassNotFoundException, UnsupportedAudioFileException, IOException {
        System.out.println("Starting up...");
//        File IF = new File("./testaudio/nosound.wav");
//        File IF = new File("./testaudio/csend.wav");
//        File IF = new File("./testaudio/hallo.wav");
        File IF = new File("./testaudio/tugip_mfp.wav");
        int IFS = (int) IF.length();
        AudioInputStream AIS = AudioSystem.getAudioInputStream(IF);
        AudioFormat AF = AIS.getFormat();

        // TODO:FIXME: enforce 2-byte Little-Endian RIFF PCM mono WAV
        double ALims = ((1000.0 * IFS) / (AF.getFrameSize() * AF.getFrameRate())); //Audio Length in miliseconds
        byte[] Buffer = new byte[IFS];
        AIS.read(Buffer);
        System.out.println("Red " + Buffer.length + " bytes from " + IF.toString() + ", should be " + ALims + " miliseconds long, framesize is " + AF.getFrameSize());

        // We will provide a visual volume feedback at 30 fps, we need an even index (2-byte sample size)
        int VisualSampleConstant = (int) (AF.getFrameRate() * AF.getFrameSize() / 30);
        if (VisualSampleConstant % 2 == 1) {
            VisualSampleConstant--;
        }

        // We need to get the minimum and the maximum so we can see if we can turn on auto-volume-gain and to determine the gain ratio
        int Min = Integer.MAX_VALUE;
        int Max = Integer.MIN_VALUE;
        short Tmp;

        ByteBuffer BB = ByteBuffer.allocate(2); // We need to have to fight with binary complements in 2-byte <=> short conversions
        BB.order(ByteOrder.LITTLE_ENDIAN);

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
            /*            if (i % (VisualSampleConstant) == 0) {
                System.err.println(i + ": " + Tmp + ":         " + Buffer[i] + " " + Buffer[i + 1]);
            }
             */        }

        System.out.println("Value min and max: " + Min + " " + Max);

        if ((Min < -1024) && (Max > 1024)) {
            int Absolut = ((Min * -1) > Max) ? (Min * -1) : Max;
            short Ratio = (short) (Short.MAX_VALUE / Absolut);
            if (Ratio > 1) {
                // TODO:FIXME:import MaxAutoVolGain from lesson config
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
                     */                }
            }
        }

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

                    for (int C = 0; C * VisualSampleConstant < Buffer.length; C++) {
//                        System.out.println("C/Level:\t"+C+"\t"+CL.getLevel()+" while "+AudioSystem.NOT_SPECIFIED); System.out.flush();
                        BB.put(0, Buffer[C * VisualSampleConstant]);
                        BB.put(1, Buffer[C * VisualSampleConstant + 1]);
                        Tmp = BB.getShort(0);
                        System.out.println("C/Level: "+C+" "+(Tmp*128/Short.MAX_VALUE)+" while "+AudioSystem.NOT_SPECIFIED);
                        try {
                            Thread.sleep(33);
                        } catch (InterruptedException IE) {
                            System.out.println("Interrupted.");
                        }
                    }
                    CL.close();
                    System.out.println("Finished playing.");
                } catch (LineUnavailableException LUE) {
                    System.err.println("LineUnavailableException: " + LUE); // LUE.printStackTrace();

                }
            }
        }
        System.out.flush();
        System.out.println("Exiting...");
    }
}
