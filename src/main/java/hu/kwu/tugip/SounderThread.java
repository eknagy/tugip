/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import static hu.kwu.tugip.Sounder.AF;
import static hu.kwu.tugip.Sounder.FPS;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SounderThread extends Thread {

    private final byte[] Buffer;
    private final SourceDataLine selectedLine;

    public boolean started = false;
    private boolean shouldEnd = false;
    private int WrittenSoFar = 0;
    public static final int SLEEPTIME = 1000 / FPS;

    public SounderThread(byte[] Buffer) {
        this.Buffer = Buffer;
        this.selectedLine = Sounder.selectedLine;
    }

    public void endMyself() {
        shouldEnd = true;
        selectedLine.close();
        App.SingletonGUI.setIntensity(0, true);
    }

    public void selfDestruct() {
        selectedLine.flush();
        endMyself();
//        System.out.println("SounderThread self-destructing.");
    }

    @Override
    public void run() {
        started=true;
//        int MinBufferSize = 48000 * 1 * 2 / 10; // 48 kHz sampling, 1-channel, 2-byte samples, 0.1 sec buffer
        try {
//            System.out.println("Planning to play on line: " + SelectedLine.getLineInfo().toString());
            selectedLine.open(AF, Buffer.length);
//            System.out.println("Line buffer size is " + selectedLine.available() + " sound buffer size is " + Buffer.length + " and MinBufferSize is " + MinBufferSize);
            int bytesToWrite = Math.min(Buffer.length - WrittenSoFar, selectedLine.available());
//            System.out.println("Initial write of " + bytesToWrite + " bytes, " + (Buffer.length - WrittenSoFar - bytesToWrite) + " bytes left.");
            selectedLine.write(Buffer, WrittenSoFar, bytesToWrite);
            WrittenSoFar += bytesToWrite;

            selectedLine.start(); // Does nothing on Debian 11?

//            double ALis = (1000*((double)Buffer.length)) / (AF.getFrameSize() * AF.getFrameRate()); //Audio Length in miliseconds
//            long MSPoffset=selectedLine.getMicrosecondPosition(); // BugFix: Starts at wrong position for mono sounds on Debian 11
//            long PrevMSP=selectedLine.getMicrosecondPosition()-MSPoffset;
//            long MSP;
            while ((selectedLine.isActive()) && (!shouldEnd)) {
                try {
                    Thread.sleep(SLEEPTIME);
                } catch (InterruptedException IE) {
                    System.out.println("Interrupted.");
                }
                if (WrittenSoFar < Buffer.length) { // BugFix: Windows 10 has tiny buffers
                    bytesToWrite = Math.min(Buffer.length - WrittenSoFar, selectedLine.available());
//                    System.out.println("Write of " + bytesToWrite + " bytes, " + (Buffer.length - WrittenSoFar - bytesToWrite) + " bytes left.");
                    selectedLine.write(Buffer, WrittenSoFar, bytesToWrite);
                    WrittenSoFar += bytesToWrite;
                } else {
//                    System.err.println("Nothing to write, draining...");
                    selectedLine.drain();
//                    System.err.println("selectedLine.drain() finished, stopping.");
                    endMyself();
                }
                /*
                // Calculate our visual sample point's position
                double Ratio=PrevMSP/1000/ALis;
                int C=(int)(Buffer.length*Ratio);

                // We need an even (as we have 2-byte frames) natural number
                if (C%2==1) {C--;}
                if (C<0) {C=0;}

                MSP=selectedLine.getMicrosecondPosition()-MSPoffset;
                if (MSP<=PrevMSP) { // BugFix: Line can remain open after playback, with invalid time/frame positions
                    break;
                } else {
                    PrevMSP=MSP;
                }
                BB.put(0, Buffer[C]);
                BB.put(1, Buffer[C+1]);
                short Tmp = BB.getShort(0);
                int VolumeLevel = Math.min(255, Math.abs(Tmp) / 32);
                App.SingletonGUI.setIntensity(VolumeLevel, true);
                 */            }
        } catch (LineUnavailableException LUE) {
            System.err.println("LineUnavailableException: " + LUE); // LUE.printStackTrace();
        }
    }
}
