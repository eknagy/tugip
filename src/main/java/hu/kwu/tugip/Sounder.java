/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import static javax.sound.sampled.AudioSystem.getMixer;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Sounder {
    public final static int FPS = 10; // Frames per second - for the sound level visualization

    ArrayList<SourceDataLine> GoodLines = new ArrayList();
    private static SourceDataLine SelectedLine = null;

    static final ByteBuffer BB; // We need not to have to fight with binary complements in 2-byte <=> short conversions

    static AudioFormat AF; // Should be file-specific, but we only accept (and enforce?) a single format anyway

    static boolean AutoVolumeEnabled = true;

    static {
        BB = ByteBuffer.allocate(2);
        BB.order(ByteOrder.LITTLE_ENDIAN);
    }

    public Sounder(String FileNameForFormatDefiniton) {
 //       File IF = new File(FileNameForFormatDefiniton);
        BufferedInputStream FIS=new BufferedInputStream(Thread.currentThread().getContextClassLoader().getResourceAsStream(FileNameForFormatDefiniton));

        try {
            AudioInputStream AIS = AudioSystem.getAudioInputStream(FIS);
            AF = AIS.getFormat();
            SelectedLine = AudioSystem.getSourceDataLine(AF);
//            System.err.println("DEBUG OVERRIDE: "+SelectedLine.toString());
/*
            DataLine.Info DLI = new DataLine.Info(SourceDataLine.class, AF);

            Mixer.Info[] Ms = AudioSystem.getMixerInfo();
            for (Mixer.Info M : Ms) {
                Mixer CM = getMixer(M);
                Line.Info[] Ls = CM.getSourceLineInfo(DLI);
                for (Line.Info CLI : Ls) {
                    SourceDataLine CL = (SourceDataLine) CM.getLine(CLI);
//                    System.out.println("Found mixer => line: " + M.toString() + " => " + CL.getLineInfo().toString());
                    try {
                        CL.open(AF, (int) (new File(FileNameForFormatDefiniton).length()));
                        CL.start();
                        CL.close();
                        GoodLines.add(CL);
                        System.out.println("Adding mixer => line " + M.toString() + " => " + CL.getLineInfo().toString() + " to GoodLines.");
                  } catch (LineUnavailableException LUE) {
                        ; // Ignore - we only add good lines to the list and ignore the rest
                    }
                }
            }
            if (GoodLines.isEmpty()) {
                throw new LineUnavailableException("GoodLines.size()==0");
            } else {
                SelectedLine = GoodLines.get(0);
                if (GoodLines.size() > 1) {
                    System.err.println("WARNING! More than one suitable SourceDataLines for playback, selecting first: ");
                    System.err.println(Arrays.toString(GoodLines.toArray()));
                    for (Mixer.Info M : Ms) {
                        System.err.println("Mixer " + M.toString() + " for " + DLI.toString() + ".");
                    }
                }
            }
*/
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException E) {
            AF = null;
            E.printStackTrace();
        }
    }

    public void playOnSelectedLine(String FileName) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        if (SelectedLine == null) {
            throw new LineUnavailableException("SelectedLine==null");
        }
        byte[] Buffer = loadWavToBuffer(FileName);   
        new SounderThread(Buffer, SelectedLine).run();
    }


    public static void autoIncreaseVolume(byte[] Buffer) {
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

        // System.out.println("Value min and max: " + Min + " " + Max);
        if ((Min < -1024) && (Max > 1024)) { // TODO:FIXME: read value from config
            int Absolut = ((Min * -1) > Max) ? (Min * -1) : Max;
            short Ratio = (short) (Short.MAX_VALUE / Absolut);
            if (Ratio > 1) {
                System.out.println("AutoVolume constant is: " + Ratio); // TODO:FIXME: read maximum permitted ratio from config
                for (int i = 0; i < Buffer.length; i += 2) {
                    BB.order(ByteOrder.LITTLE_ENDIAN);
                    BB.put(0, Buffer[i]);
                    BB.put(1, Buffer[i + 1]);
                    Tmp = BB.getShort(0);
                    BB.putShort(0, (short) (Tmp * Ratio));
                    Buffer[i] = BB.get(0);
                    Buffer[i + 1] = BB.get(1);
                }
            }
        }
    }

    public static byte[] loadWavToBuffer(String FileName) throws UnsupportedAudioFileException, IOException {
//        System.err.println("DEBUG SOUNDER1: "+ Thread.currentThread().getContextClassLoader().getResource (FileName));
//        File IF = new File(FileName); System.err.println("DEBUG SOUNDER2: "+ IF.toString());
        BufferedInputStream FIS=new BufferedInputStream(Thread.currentThread().getContextClassLoader().getResourceAsStream(FileName));
        AudioInputStream AIS = AudioSystem.getAudioInputStream(FIS);
        AudioFormat MAF = AIS.getFormat();
        if (!AF.toString().equals(MAF.toString())) {
            throw new UnsupportedAudioFileException("AF!=MAF: " + AF.toString() + " and " + MAF.toString());
        }

        byte [] Buffer = new byte[AIS.available()];
        double ALims = ((1000.0 * Buffer.length) / (AF.getFrameSize() * AF.getFrameRate())); //Audio Length in miliseconds
        AIS.read(Buffer);
        System.out.println("Red " + Buffer.length + " bytes from " + FIS.toString() + ", should be " + ALims + " miliseconds long, framesize is " + AF.getFrameSize());

        int MyVisualSampleConstant = (int) (AF.getFrameRate() * AF.getFrameSize() / FPS);
        if (MyVisualSampleConstant % 2 == 1) {
            MyVisualSampleConstant--;
        }

        if (AutoVolumeEnabled) {
            autoIncreaseVolume(Buffer);
        }

        return Buffer;
    }

}
