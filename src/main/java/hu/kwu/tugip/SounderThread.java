/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import static hu.kwu.tugip.Sounder.AF;
import static hu.kwu.tugip.Sounder.BB;
import static hu.kwu.tugip.Sounder.FPS;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SounderThread extends Thread {
    byte[] Buffer;
    SourceDataLine SelectedLine;
    
    public SounderThread(byte[] Buffer, SourceDataLine SelectedLine) {
        this.Buffer=Buffer;
        this.SelectedLine=SelectedLine;
    }

    @Override
    public void run() {
        int MinBufferSize=48000*1*2/10; // 48 kHz sampling, 1-channel, 2-byte samples, 0.1 sec buffer
        int WrittenSoFar=0;
        int SleepTime=1000/FPS;
        try {
            System.out.println("Planning to play on line: " + SelectedLine.getLineInfo().toString());
            SelectedLine.open(AF, Buffer.length);
            System.out.println("Line buffer size is " + SelectedLine.available() + " sound buffer size is " + Buffer.length+" and MinBufferSize is "+MinBufferSize);
            int bytesToWrite=Math.min(Buffer.length-WrittenSoFar, SelectedLine.available());
//            System.out.println("Initial write of " + bytesToWrite + " bytes, "+(Buffer.length-WrittenSoFar-bytesToWrite)+" bytes left.");
            SelectedLine.write(Buffer, WrittenSoFar, bytesToWrite);
            WrittenSoFar+=bytesToWrite;

            SelectedLine.start();

            double ALis = (1000*((double)Buffer.length)) / (AF.getFrameSize() * AF.getFrameRate()); //Audio Length in miliseconds

            long MSPoffset=SelectedLine.getMicrosecondPosition(); // BugFix: Starts at wrong position for mono sounds on Debian 11

            long PrevMSP=SelectedLine.getMicrosecondPosition()-MSPoffset;
            long MSP;

            while (SelectedLine.isActive()) {
                try {
                    Thread.sleep(SleepTime);
                } catch (InterruptedException IE) {
                    System.out.println("Interrupted.");
                }
                if (WrittenSoFar<Buffer.length) { // BugFix: Windows 10 has tiny buffers
                    bytesToWrite=Math.min(Buffer.length-WrittenSoFar, SelectedLine.available());
//                    System.out.println("Write of " + bytesToWrite + " bytes, "+(Buffer.length-WrittenSoFar-bytesToWrite)+" bytes left.");
                    SelectedLine.write(Buffer, WrittenSoFar, bytesToWrite);
                    WrittenSoFar+=bytesToWrite;
                }

                // Calculate our visual sample point's position
                double Ratio=PrevMSP/1000/ALis;
                int C=(int)(Buffer.length*Ratio);

                // We need an even (as we have 2-byte frames) natural number
                if (C%2==1) {C--;}
                if (C<0) {C=0;}

                MSP=SelectedLine.getMicrosecondPosition()-MSPoffset;
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

//                System.err.println("C is "+C+" and SL.gFP() is "+SelectedLine.getFramePosition()+" and SL.gMP() is " + SelectedLine.getMicrosecondPosition()+" and status is "+SelectedLine.isRunning()+" and "+SelectedLine.isActive());
            }
            SelectedLine.flush();
            SelectedLine.close();
            System.out.println("Finished playing.");
        } catch (LineUnavailableException LUE) {
            System.err.println("LineUnavailableException: " + LUE); // LUE.printStackTrace();
        }
        App.SingletonGUI.setIntensity(0, true);
    }

}
