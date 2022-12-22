/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.swing.JLabel;

public class Lecturer {

    private final Properties P = new Properties();
    private final String lectureName;
    private final String wavDir;
    public int passPercent = 80;
    public int badCount = 0;
    public int goodCount = 0;

    public int getCurrentPercent() {
        return (((badCount + goodCount) == 0) ? -1 : (100 * goodCount) / (badCount + goodCount));
    }

    public void regeneratePassPanel(JLabel passLabel) {
        int currentPercent = getCurrentPercent();
        if (currentPercent < 0) {
            passLabel.setText("??% (" + passPercent + "%)");
            passLabel.setBackground(Color.WHITE);
        } else {
            passLabel.setText("" + currentPercent + "% (" + passPercent + "%)");
            passLabel.setBackground((currentPercent >= passPercent) ? Color.GREEN : Color.RED);
        }
    }

    public Lecturer(String lectureName) throws IOException {
        this.lectureName = lectureName;
        try {
            BufferedInputStream FIS = new BufferedInputStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("lectures/" + this.lectureName + "/config.properties"));
            P.load(FIS);
        } catch (IOException E) {
            System.err.println("Lecturer(" + this.lectureName + ") Exception: " + E.toString());
            throw new IOException(E);
        }
        wavDir = "lecturesounds/" + P.getProperty("WAVDIR", "undefined") + "/";
        try {
            passPercent = Integer.parseInt(P.getProperty("PASS_PERCENT", "80").trim());
        } catch (NumberFormatException NFE) {
            System.err.println("passPercent defaulting to 80 because of NFE: " + NFE.toString());
        }
    }

    public String getWavDir() {
        return wavDir;
    }

    public String getNextLine() {
        return P.getProperty("TEXT_1", "");
    }

    public String[] getHelloFilesNames() throws IOException {
        /*        String HelloPathString = "lectures/" + this.LectureName + "/" + P.getProperty("BEFORE_LECTURE") + ".wav";
        Path HelloPath = Paths.get(HelloPathString);
        if ((!Files.exists(HelloPath)) || (!Files.isReadable(HelloPath)) || (!Files.isRegularFile(HelloPath))) {
              throw new IOException("Not {found, readable, a file}: "+HelloPath.toString()+" ("+HelloPathString+")");
        }
         */
        String helloFilesLine = P.getProperty("BEFORE_LECTURE", "hello");
        String[] helloFilesStrings = helloFilesLine.split("\\s");
        String[] helloFilesNames = new String[helloFilesStrings.length];

        for (int i = 0; i < helloFilesStrings.length; i++) {
            helloFilesNames[i] = "lectures/" + this.lectureName + "/" + helloFilesStrings[i] + ".wav";
        }

        return (helloFilesNames);
    }
}
