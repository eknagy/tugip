/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import static hu.kwu.tugip.App.SingletonGUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class GUI extends JFrame {

    private final static String PreText = "<html><div style='text-align: center;'><div style='background-color: green'>";
    private final static String PostText = "</div><html>";
    private final static String BeforeTargetLetter = "</div><div style='background-color: yellow'>";
    private final static String AfterTargetLetter = "</div>";

    private final static String TextToType = "fff jjj fjfjfj";
    private static int TextTypedPosition = 0;

    private static int GoodKeyCode = KeyEvent.VK_SPACE;

    private static boolean acceptInput = false;

    private static Sounder S;

    private JPanel VisualiserPanel = new JPanel();
    private JPanel TextPanel = new JPanel(new BorderLayout());
    private JLabel TextLabel = new JLabel(PreText + PostText);
    private JPanel ProgressPanel = new JPanel();
    private Color[] colorTable = new Color[2 * 256];
    private HashSet<SounderThread> MySoundThreads = new HashSet<SounderThread>();

    public void regenerateText() {
        if (TextToType.length() == 0) {
            return;
        }
        try {
            System.err.println("DEBUG: " + TextTypedPosition + " " + TextToType.substring(0, TextTypedPosition));
            TextLabel.setText(PreText + TextToType.substring(0, TextTypedPosition)
                    + BeforeTargetLetter + TextToType.substring(TextTypedPosition, TextTypedPosition + 1)
                    + AfterTargetLetter + TextToType.substring(TextTypedPosition + 1) + PostText);
        } catch (IndexOutOfBoundsException I) {
            System.err.println("DEBUG: " + TextTypedPosition + " IOOBE");
            TextLabel.setText(PreText + TextToType + PostText);
        }
        TextLabel.updateUI();
    }

    public void registerSounderThread(SounderThread ST) {
        MySoundThreads.add(ST);
        System.err.println("DEBUG: registering ST: " + ST.toString());
    }

    public void unRegisterSounderThread(SounderThread ST) {
        MySoundThreads.remove(ST);
        System.err.println("DEBUG: deregistering ST: " + ST.toString());
    }

    public void notifySounderThreads(int KeyCode) {
        for (SounderThread ST : MySoundThreads) {
            ST.keyDown(KeyCode);
            System.err.println("DEBUG: notifying with " + KeyCode + " ST: " + ST.toString());
        }
    }

    public void processKeyCode(int KeyCode) {
        if (KeyCode == GoodKeyCode) {
            notifySounderThreads(KeyCode);
            if (acceptInput) {
                TextTypedPosition++;
                regenerateText();
                if (TextTypedPosition == TextToType.length()) {
                    S.playOnSelectedLine("systemsounds/yuhuu.wav", KeyEvent.VK_SPACE, true);
                    close();
                } else {
                    prepareGoodKeyCode();
                }
            }
        } else {
            S.syncPlayOnSelectedLine("systemsounds/hiba.wav", -1);
        }
    }

    public void startLecture() {
        acceptInput = true;
        regenerateText();
        prepareGoodKeyCode();
    }

    public void prepareGoodKeyCode() {
        String NextChar = TextToType.substring(TextTypedPosition, TextTypedPosition + 1);
        switch (NextChar) {
            case " ":
                NextChar = "space";
                GoodKeyCode = KeyEvent.VK_SPACE;
                break;
            case "j":
                GoodKeyCode = KeyEvent.VK_J;
                break;
            case "f":
                GoodKeyCode = KeyEvent.VK_F;
                break;
            default:
                System.err.println("Error: unhandled next char in Lecturer: " + NextChar);
        }

        S.playOnSelectedLine(App.L.getWavDir() + NextChar + ".wav", GoodKeyCode, false);
    }

    public void setIntensity(int Value, boolean isGreen) {
        if ((Value < 0) || (Value > 255)) {
            throw new RuntimeException("GUI.setIntensity() Value is " + Value);
        }
        VisualiserPanel.setBackground(colorTable[Value + (isGreen ? 0 : 256)]);
        VisualiserPanel.updateUI();
    }

    public GUI(Sounder S) {
        this.S = S;
        for (int i = 0; i < 256; i++) {
            colorTable[i] = new Color(i << 8);
            colorTable[i + 256] = new Color(i << 16);
        }
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        GridBagLayout GBL = new GridBagLayout();
        this.setLayout(GBL);
        GridBagConstraints GBC = new GridBagConstraints();
        GBC.gridx = 1;
        GBC.fill = GridBagConstraints.BOTH;
        GBC.weightx = 1;
        GBC.weighty = 3;
        this.add(VisualiserPanel, GBC);
        GBC.weighty = 9;
        this.add(TextPanel, GBC);
        GBC.weighty = 1;
        this.add(ProgressPanel, GBC);
        pack();
        setVisible(true);
        TextPanel.setBackground(Color.white);
        TextPanel.add(TextLabel, BorderLayout.CENTER);
//        TextLabel.setBackground(Color.yellow);
//        TextLabel.setOpaque(true);
        TextLabel.setFont(new Font("Monospaced", Font.BOLD, 144));
        TextLabel.setVerticalAlignment(SwingConstants.CENTER);
        TextLabel.setHorizontalAlignment(SwingConstants.CENTER);
        ProgressPanel.setBackground(Color.MAGENTA);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent KE) {
                if (KE.getID() == KeyEvent.KEY_PRESSED) {
                    switch (KE.getKeyCode()) {
                        case KeyEvent.VK_SPACE:
                        case KeyEvent.VK_F:
                        case KeyEvent.VK_J:
                            System.err.println("DEBUG: Sending KeyCode: " + KE.getKeyCode());
                            processKeyCode(KE.getKeyCode());
                            break;
                        default:
                            ; // Ignore
                    }
                    return (true);
                } else if (KE.getID() == KeyEvent.KEY_PRESSED) {
                    System.err.println("DEBUG: Unhandled KeyEvent: " + KE.toString());
                    return (true);
                }
                return (false);
            }
        });
    }

    public void close() {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));;
    }
}
