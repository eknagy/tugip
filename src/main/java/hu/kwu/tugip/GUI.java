/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import static hu.kwu.tugip.App.L;
import static hu.kwu.tugip.Director.generateNumberFileNames;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.TextArea;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class GUI extends JFrame {

    public static boolean debugMode = false;

    private final static String PRETEXT = "<html><div style='text-align: center;'><div style='background-color: #C0FFC0'>";
    private final static String POSTTEXT = "</div><html>";
    private final static String BEFORETARGET = "</div><div style='background-color: yellow'>";
    private final static String AFTERTARGET = "</div>";

    private static String textToType = "";
    private static int textTypedPosition = 0;

    private final static Font FONT144 = new Font("Monospaced", Font.BOLD, 48);
    private final static Font FONT36 = new Font("Monospaced", Font.BOLD, 36);

    private static boolean acceptInput = false;

    private final JPanel northPanel = new JPanel(new GridLayout(1, 4));
    private final JLabel goodPointCount = new JLabel("0", SwingConstants.CENTER);
    private final JLabel badPointCount = new JLabel("0", SwingConstants.CENTER);
    private final JLabel passLabel = new JLabel("??% (??%)", SwingConstants.CENTER);
    private final JPanel visualiserPanel = new JPanel();
    private final JPanel textPanel = new JPanel(new BorderLayout());
    private final JPanel debugPanel = new JPanel(new BorderLayout());
    private final JLabel textLabel = new JLabel(PRETEXT + POSTTEXT);
    private final JPanel aboutPanel = new JPanel(new GridLayout(2, 1));
    private final Color[] colorTable = new Color[2 * 256];

    public final TextArea D = new TextArea("DEBUG: Loading...");
    public final TextArea DI = new TextArea("DEBUG: Loading...");

    public void regenerateText() {
        if (textToType.length() == 0) {
            return;
        }
        try {
            System.err.println("DEBUG: tTP: " + textTypedPosition + " " + textToType.substring(0, textTypedPosition));
            textLabel.setText(PRETEXT + textToType.substring(0, textTypedPosition)
                    + BEFORETARGET + textToType.substring(textTypedPosition, textTypedPosition + 1)
                    + AFTERTARGET + textToType.substring(textTypedPosition + 1) + POSTTEXT);
        } catch (IndexOutOfBoundsException I) {
//            System.err.println("DEBUG: " + textTypedPosition + " IOOBE");
            textLabel.setText(PRETEXT + textToType + POSTTEXT);
        }
        textLabel.updateUI();
    }

    public void processKeyCode(int keyCode) {
        DI.setText(DI.getText() + " " + keyCode);
        if (acceptInput) {
            if (Director.consumeKeyDown(keyCode)) {
                textTypedPosition++;
                regenerateText();
            }
            goodPointCount.setText("" + L.goodCount);
            badPointCount.setText("" + L.badCount);
            L.regeneratePassPanel(passLabel);
            Director.playFirst();

        }
    }

    public void startLecture(boolean nextLineMode) {
        String[] helloFileNames = new String[0];

        textTypedPosition = 0;

        if (nextLineMode) {
            textToType = L.getNextLine();
        } else {
            textToType = L.getCurrentLine();
            try {
                helloFileNames = L.getHelloFilesNames();
            } catch (IOException E) {
                App.redAlert(E.toString());
            }
            L.resetCounts();
        }

        regenerateText();

        L.regeneratePassPanel(passLabel);

        Director.addNew(null, -3); // Director uses a stack (FILO) - "generate results" command first
        
        /*        if (true) {
            Stack<String> tmpStack = new Stack<>();

            Director.addNew(App.SYSTEMSOUNDDIR+"hello.wav", -2);
            for (int i = 100; i >=0; i--) {
                for (String CS : generateNumberFileNames(i)) {
                    Director.addNew(App.NUMBERSSOUNDDIR + CS + ".wav", -2);
                }
            }
            generateEnding(1,2,4,3);
            acceptInput = true;
            Director.play();
            return;
        }
         */

        int targetKeyCode = -1;

        for (int i = textToType.length() - 1; i >= 0; i--) {
            String nextChar = textToType.substring(i, i + 1);
            switch (nextChar) {
                case "\u23CE":
                    nextChar = "enter";
                    targetKeyCode = KeyEvent.VK_ENTER;
                    break;
                case " ":
                    nextChar = "space";
                    targetKeyCode = KeyEvent.VK_SPACE;
                    break;
                case "a":
                    targetKeyCode = KeyEvent.VK_A;
                    break;
                case "d":
                    targetKeyCode = KeyEvent.VK_D;
                    break;
                case "é":
                    targetKeyCode = 16777449;
                    break;
                case "f":
                    targetKeyCode = KeyEvent.VK_F;
                    break;
                case "j":
                    targetKeyCode = KeyEvent.VK_J;
                    break;
                case "k":
                    targetKeyCode = KeyEvent.VK_K;
                    break;
                case "l":
                    targetKeyCode = KeyEvent.VK_L;
                    break;
                case "s":
                    targetKeyCode = KeyEvent.VK_S;
                    break;
                default:
                    System.err.println("Error: unhandled next char in GUI: " + nextChar);
            }
//            System.err.println("DEBUG: " + nextChar + " " + targetKeyCode);
            Director.addNew(nextChar + ".wav", targetKeyCode);
        }

        if (!nextLineMode) {
            for (int h = helloFileNames.length - 1; h >= 0; h--) {
                Director.addNew(helloFileNames[h], -1);
            }
            /*
            for (String S : generateNumberFileNames(199)) {
                Director.addNew(S+".wav", -1);
            }
             */
            Director.addNew("hello.wav", -1);
        }
        acceptInput = true;

        Director.playFirst();
    }

    public void setIntensity(int Value, boolean isGreen) {
        if ((Value < 0) || (Value > 255)) {
            throw new RuntimeException("GUI.setIntensity() Value is " + Value);
        }
        visualiserPanel.setBackground(colorTable[Value + (isGreen ? 0 : 256)]);
        visualiserPanel.updateUI();
    }

    public void registerKeyHandler() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent KE) {
                if (KE.getID() == KeyEvent.KEY_PRESSED) {
                    switch (KE.getExtendedKeyCode()) {
                        case KeyEvent.VK_SPACE:
                        case KeyEvent.VK_ENTER:
                        case KeyEvent.VK_ESCAPE:
                        case KeyEvent.VK_A:
                        case KeyEvent.VK_D:
                        case KeyEvent.VK_F:
                        case KeyEvent.VK_J:
                        case KeyEvent.VK_K:
                        case KeyEvent.VK_L:
                        case KeyEvent.VK_S:
                        case 16777449: // Hungarian é
                            processKeyCode(KE.getExtendedKeyCode());
                            break;
                        case KeyEvent.VK_CAPS_LOCK:
                        case KeyEvent.VK_SHIFT:
                            textPanel.setBackground(textPanel.getBackground() == Color.WHITE ? Color.CYAN : Color.WHITE);
                            break;
                        default:
                            System.err.println("DEBUG: Unknown KeyEvent: " + KE + " or " + (0 + KE.getExtendedKeyCode()));
                            ; // Ignore
                    }
                    return (true);
                } else if ((KE.getID() == KeyEvent.KEY_RELEASED) && (KE.getExtendedKeyCode() == KeyEvent.VK_SHIFT)) {
                    textPanel.setBackground(textPanel.getBackground() == Color.WHITE ? Color.CYAN : Color.WHITE);
                }
                return (false);
            }
        });

    }

    public GUI(Sounder S) {
        setTitle("TUGIP "+App.VERSION);
        for (int i = 0; i < 256; i++) {
            colorTable[i] = new Color(i << 8);
            colorTable[i + 256] = new Color(i << 16);
        }
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);

        goodPointCount.setForeground(Color.GREEN);
        goodPointCount.setBackground(Color.WHITE);
        goodPointCount.setOpaque(true);
        goodPointCount.setFont(FONT36);
        northPanel.add(goodPointCount);
        badPointCount.setForeground(Color.RED);
        badPointCount.setBackground(Color.WHITE);
        badPointCount.setOpaque(true);
        badPointCount.setFont(FONT36);
        northPanel.add(badPointCount);
        passLabel.setForeground(Color.BLACK);
        passLabel.setBackground(Color.WHITE);
        passLabel.setOpaque(true);
        passLabel.setFont(FONT36);
        northPanel.add(passLabel);
        northPanel.add(visualiserPanel);

        GridBagLayout GBL = new GridBagLayout();
        this.setLayout(GBL);
        GridBagConstraints GBC = new GridBagConstraints();
        GBC.gridx = 1;
        GBC.fill = GridBagConstraints.BOTH;
        GBC.weightx = 1;
        GBC.weighty = 10;
        this.add(northPanel, GBC);
        GBC.weighty = 20;
        this.add(textPanel, GBC);
        GBC.weighty = 1;
        this.add(aboutPanel, GBC);
        aboutPanel.setBackground(Color.LIGHT_GRAY);

        JPanel aboutUpperPanel = new JPanel(new GridLayout(1, 4));
        JPanel aboutLowerPanel = new JPanel(new GridLayout(1, 2));
        aboutPanel.add(aboutUpperPanel);
        aboutPanel.add(aboutLowerPanel);
        aboutUpperPanel.add(new JLabel("Tugip v. "+App.VERSION, SwingConstants.CENTER));
        aboutUpperPanel.add(new JLabel("Gépírás tankönyv: Rácz Hajnalka", SwingConstants.CENTER));
        aboutUpperPanel.add(new JLabel("Projektmenedzser: Dr. Nógrádi Judit", SwingConstants.CENTER));
        aboutUpperPanel.add(new JLabel("Szoftverfejlesztő: Dr. Nagy Elemér Károly", SwingConstants.CENTER));
        aboutLowerPanel.add(new JLabel("A projektet a Gyengénlátók Általános Iskolája, EGYMI és Kollégiuma támogatta.", SwingConstants.CENTER));
        aboutLowerPanel.add(new JLabel("A projektet az FSF.hu Alapítvány a Szabad Szoftver Pályázat 2022 keretén belül támogatta.", SwingConstants.CENTER));
        textPanel.setBackground(Color.WHITE);
        textPanel.add(textLabel, BorderLayout.CENTER);

        textLabel.setFont(FONT144);
        textLabel.setVerticalAlignment(SwingConstants.CENTER);
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textLabel.setPreferredSize(new Dimension(1000, 600));

        if (debugMode) {
            textPanel.add(debugPanel, BorderLayout.NORTH);

        }
        debugPanel.setBackground(Color.yellow);
        D.setEditable(false);
        D.setPreferredSize(new Dimension(1000, 300));
        DI.setEditable(false);
        DI.setPreferredSize(new Dimension(1000, 50));
        debugPanel.add(D, BorderLayout.CENTER);
        debugPanel.add(DI, BorderLayout.SOUTH);
        pack();
        setVisible(true);
        textLabel.requestFocusInWindow();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        registerKeyHandler();
    }

    public void close() {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));;
    }
}
