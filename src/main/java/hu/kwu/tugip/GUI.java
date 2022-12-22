/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class GUI extends JFrame {

    private final static String PRETEXT = "<html><div style='text-align: center;'><div style='background-color: #C0FFC0'>";
    private final static String POSTTEXT = "</div><html>";
    private final static String BEFORETARGET = "</div><div style='background-color: yellow'>";
    private final static String AFTERTARGET = "</div>";

    private static String textToType = "";
    private static int textTypedPosition = 0;

    private static boolean acceptInput = false;

    private JPanel VisualiserPanel = new JPanel();
    private JPanel TextPanel = new JPanel(new BorderLayout());
    private JLabel TextLabel = new JLabel(PRETEXT + POSTTEXT);
    private JPanel AboutPanel = new JPanel();
    private Color[] colorTable = new Color[2 * 256];

    public void regenerateText() {
        if (textToType.length() == 0) {
            return;
        }
        try {
            // System.err.println("DEBUG: " + textTypedPosition + " " + textToType.substring(0, textTypedPosition));
            TextLabel.setText(PRETEXT + textToType.substring(0, textTypedPosition)
                    + BEFORETARGET + textToType.substring(textTypedPosition, textTypedPosition + 1)
                    + AFTERTARGET + textToType.substring(textTypedPosition + 1) + POSTTEXT);
        } catch (IndexOutOfBoundsException I) {
            // System.err.println("DEBUG: " + textTypedPosition + " IOOBE");
            TextLabel.setText(PRETEXT + textToType + POSTTEXT);
        }
        TextLabel.updateUI();
    }

    public void processKeyCode(int keyCode) {
//        System.err.println("DEBUG: processKeyCode: in: "+keyCode);
        if (acceptInput) {
            if (Director.consumeKeyDown(keyCode)) {
                textTypedPosition++;
                regenerateText();
            }
        }
    }

    public void startLecture(String[] helloFileNames) {
        textToType = App.L.getNextLine();
        regenerateText();

        int targetKeyCode = -1;

        Director.addNew("systemsounds/yuhuu.wav", -2); // Director uses a stack (FILO) - victory sound first

        for (int i = textToType.length() - 1; i >= 0; i--) {
            String nextChar = textToType.substring(i, i + 1);
            switch (nextChar) {
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
            Director.addNew(App.L.getWavDir() + nextChar + ".wav", targetKeyCode);
        }

        for (String CS : helloFileNames) {
            Director.addNew(CS, -1);
        }

        acceptInput = true;
        Director.play();
    }

    public void setIntensity(int Value, boolean isGreen) {
        if ((Value < 0) || (Value > 255)) {
            throw new RuntimeException("GUI.setIntensity() Value is " + Value);
        }
        VisualiserPanel.setBackground(colorTable[Value + (isGreen ? 0 : 256)]);
        VisualiserPanel.updateUI();
    }

    public GUI(Sounder S) {
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
        this.add(AboutPanel, GBC);
        AboutPanel.setBackground(Color.LIGHT_GRAY);

        AboutPanel.setLayout(new GridLayout(2,1));
        JPanel AboutUpperPanel=new JPanel(new GridLayout(1,4));
        JPanel AboutLowerPanel=new JPanel(new GridLayout(1,2));
        AboutPanel.add(AboutUpperPanel);
        AboutPanel.add(AboutLowerPanel);
        AboutUpperPanel.add(new JLabel("Tugip v. 0.1.0", SwingConstants.CENTER));
        AboutUpperPanel.add(new JLabel("Gépírás tankönyv: Rácz Hajnalka", SwingConstants.CENTER));
        AboutUpperPanel.add(new JLabel("Projektmenedzser: Dr. Nógrádi Judit", SwingConstants.CENTER));
        AboutUpperPanel.add(new JLabel("Szoftverfejlesztő: Dr. Nagy Elemér Károly", SwingConstants.CENTER));
        AboutLowerPanel.add(new JLabel("A projektet a Gyengénlátók Általános Iskolája, EGYMI és Kollégiuma támogatta.",SwingConstants.CENTER));
        AboutLowerPanel.add(new JLabel("A projektet az FSF.hu Alapítvány a Szabad Szoftver Pályázat 2022 keretén belül támogatta.",SwingConstants.CENTER));
        TextPanel.setBackground(Color.white);
        TextPanel.add(TextLabel, BorderLayout.CENTER);
        TextLabel.setFont(new Font("Monospaced", Font.BOLD, 144));
        TextLabel.setVerticalAlignment(SwingConstants.CENTER);
        TextLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pack();
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent KE) {
                if (KE.getID() == KeyEvent.KEY_PRESSED) {
                    switch (KE.getExtendedKeyCode()) {
                        case KeyEvent.VK_SPACE:
                        case KeyEvent.VK_A:
                        case KeyEvent.VK_D:
                        case KeyEvent.VK_F:
                        case KeyEvent.VK_J:
                        case KeyEvent.VK_K:
                        case KeyEvent.VK_L:
                        case KeyEvent.VK_S:
                        case 16777449: // Hungarian é

//                            System.err.println("DEBUG: Sending KeyCode: " + KE.getKeyCode());
                            processKeyCode(KE.getExtendedKeyCode());
                            break;
                        default:
                            System.err.println("DEBUG: Unknown KeyEvent: " + KE + " or " + (0 + KE.getExtendedKeyCode()));
                            ; // Ignore
                    }
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
