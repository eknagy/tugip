/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

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
    public final static int FPS = 25; // Frames per second - for the sound level visualization

    ArrayList<SourceDataLine> GoodLines = new ArrayList();
    private static SourceDataLine SelectedLine = null;

    static final ByteBuffer BB; // We need not to have to fight with binary complements in 2-byte <=> short conversions

    static AudioFormat AF; // Should be file-specific, but we only accept (and enforce?) a single format anyway

    static int VisualSampleConstant = -1; // To provide a visual volume feedback at 30 fps, we need index increment value (2-byte sample size => even)

    static boolean AutoVolumeEnabled = true;

    static {
        BB = ByteBuffer.allocate(2);
        BB.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void playOnSelectedLine(String FileName, GUI MyGUI) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
        if (SelectedLine == null) {
            throw new LineUnavailableException("SelectedLine==null");
        }
        byte[] Buffer = loadWavToBuffer(FileName);

        try {
            System.out.println("Playing on line: " + SelectedLine.getLineInfo().toString());
            SelectedLine.open(AF, Buffer.length);
            SelectedLine.write(Buffer, 0, Buffer.length);

            for (int C = 0; C * VisualSampleConstant < Buffer.length; C++) {
                BB.put(0, Buffer[C * VisualSampleConstant]);
                BB.put(1, Buffer[C * VisualSampleConstant + 1]);
                short Tmp = BB.getShort(0);
                int VolumeLevel = Math.min(255, Math.abs(Tmp) / 32);
                MyGUI.setIntensity(VolumeLevel, true);

                try {
                    Thread.sleep(1000 / FPS);
                } catch (InterruptedException IE) {
                    System.out.println("Interrupted.");
                }
            }
            SelectedLine.close();
            System.out.println("Finished playing.");
            MyGUI.close();
        } catch (LineUnavailableException LUE) {
            System.err.println("LineUnavailableException: " + LUE); // LUE.printStackTrace();
        }
        MyGUI.setIntensity(255, false);
    }

    public Sounder(String FileNameForFormatDefiniton) {
        File IF = new File(FileNameForFormatDefiniton);
        try {
            AudioInputStream AIS = AudioSystem.getAudioInputStream(IF);
            AF = AIS.getFormat();

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
                        CL.close();
                        GoodLines.add(CL);
//                        System.out.println("Adding mixer => line " + M.toString() + " => " + CL.getLineInfo().toString() + " to GoodLines.");
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
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException E) {
            AF = null;
            E.printStackTrace();
        }
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
        File IF = new File(FileName);
        int IFL = (int) IF.length();
        AudioInputStream AIS = AudioSystem.getAudioInputStream(IF);
        AudioFormat MAF = AIS.getFormat();
        if (!AF.toString().equals(MAF.toString())) {
            throw new UnsupportedAudioFileException("AF!=MAF: " + AF.toString() + " and " + MAF.toString());
        }

        double ALims = ((1000.0 * IFL) / (AF.getFrameSize() * AF.getFrameRate())); //Audio Length in miliseconds
        byte[] Buffer = new byte[IFL];
        AIS.read(Buffer);
        System.out.println("Red " + Buffer.length + " bytes from " + IF.toString() + ", should be " + ALims + " miliseconds long, framesize is " + AF.getFrameSize());

        int MyVisualSampleConstant = (int) (AF.getFrameRate() * AF.getFrameSize() / FPS);
        if (MyVisualSampleConstant % 2 == 1) {
            MyVisualSampleConstant--;
        }
        if (VisualSampleConstant == -1) {
            VisualSampleConstant = MyVisualSampleConstant;
        } else if (VisualSampleConstant != MyVisualSampleConstant) {
            throw new UnsupportedAudioFileException("VisualSampleConstant (bitrate) mismatch!");
        }

        if (AutoVolumeEnabled) {
            autoIncreaseVolume(Buffer);
        }

        return Buffer;
    }

}
