package app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;

import model.Renderer;
import com.sun.opengl.util.Animator;

public class OstrovApp extends JFrame implements ActionListener{
	JFrame jframe = new JFrame("Ostrov");
	JButton btn = new JButton("N�pov�da");
	JButton btn1 = new JButton("Zad�n� pr�ce");
	JButton btn2 = new JButton("Zav��t aplikaci");
	JTextArea tp = new JTextArea();
	JTextField tf = new JTextField();
	JPanel horni = new JPanel();
	JSlider teren = new JSlider(1, 160);
	GraphicsDevice Obrazovka = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
	int sirka = Obrazovka.getDisplayMode().getWidth();
	int vyska = Obrazovka.getDisplayMode().getHeight();
	int sirkaPlatna = 800;
	int vyskaPlatna = 600;
	int poziceY;
	int poziceX;
	GLCanvas platno = new GLCanvas();
	final Animator animator = new Animator(platno);
	 
	/**
	 * @param args
	 */
	public OstrovApp() {
		int fullscreen = JOptionPane.showConfirmDialog(null, "Chcete spustit aplikaci v celoobrazovem modu?",
				"Fullscreen", JOptionPane.YES_NO_CANCEL_OPTION);
	
		switch(fullscreen){
    		case 0:
    		jframe.setSize(sirka,vyska);
    		jframe.setUndecorated(true);
    		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(jframe);
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setDisplayMode(new DisplayMode(sirka,vyska, 32, DisplayMode.REFRESH_RATE_UNKNOWN));
			try {
				Obrazovka.setFullScreenWindow(jframe);
				}
			finally {
				Obrazovka.setFullScreenWindow(null);
			}
		break;
    		case 1:
			jframe.setSize(800, 600);
			poziceX = (sirka - sirkaPlatna)>>1;
			poziceY = (vyska - vyskaPlatna)>>1;
			//pokud neni pusteno ve fullscreenu bude posunuto od okraje obrazovky
			jframe.setLocation(poziceX,poziceY);
			jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		break;
    		case 2:
    		animator.stop();
    		System.exit(0);
    	break;
		}
		
		platno.requestFocus();
		Renderer ren = new Renderer();
		
		GLCapabilities glCaps = new GLCapabilities();
	   	glCaps.setRedBits(8);
		glCaps.setBlueBits(8);
		glCaps.setGreenBits(8);
		glCaps.setAlphaBits(8);
		glCaps.setDepthBits(24);
		
		jframe.add(platno, BorderLayout.CENTER);
		jframe.add(horni, BorderLayout.NORTH);
		horni.setLayout(new FlowLayout((int) LEFT_ALIGNMENT));
		horni.add(btn);
		horni.add(btn1);
		horni.add(btn2);
		btn.addActionListener(this);
		btn1.addActionListener(this);
		btn2.addActionListener(this);		

		
		
		platno.addGLEventListener(ren);
	
		jframe.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				animator.stop();
				System.exit(0);
			}
		});
		
		jframe.setVisible(true);
		animator.start();
		animator.setRunAsFastAsPossible(true);
		platno.requestFocus();
	}
	public static void main(String[] args) {
		try {
				OstrovApp ostrovApp = new OstrovApp();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		poziceX = (sirka - sirkaPlatna)>>1;
		poziceY = (vyska - vyskaPlatna)>>1;
		if (e.getSource().equals(btn)) {
			JFrame napoveda = new JFrame("N�pov�da");
			napoveda.setVisible(true);
			napoveda.add(tp);
			tp.setBackground(new Color(0.95f, 0.95f, 0.95f));
			tp.setText("Ovl�d�n�: \nPohyb: W/S/A/D \n" + "Pohled: tla��tko my�i + pohyb my�� \n\n" +
					"Funkce: \n" + " 'T': zapnout/vypnout textury \n " +
					"'L': zapnout/vypnout osv�tlen� \n " +
					"'F': re�im l�t�n�/pohled z mola(bez chozen�)\n" +
					"'R': dr�t�n� model\n" +
					"'B': zapnout/vypnout pr�hlednost \n" +
					"�ipka vlevo/vpravo: LOD vody \n �ipka nahoru/dolu: LOD ter�nu");
			tp.setEditable(false);
			napoveda.setLocation(poziceX, poziceY);
			napoveda.pack();
		} else if (e.getSource().equals(btn1)) {
			JFrame o = new JFrame("Zad�n� pr�ce");
			JTextArea ta = new JTextArea();
			o.add(ta);
			ta.setEditable(false);
			ta.setBackground(new Color(0.95f, 0.95f, 0.95f));
			ta.setText("Vytvo�il: Ale� Jir�sek \n\n" +
					"Obor: AI2-KF    	Ro�n�k: 1. \n\n" +
					"Datum posledni �pravy: 28.8.2012\n\n" +
					"Zad�n�: Vytvo�it model ostrov� s pr�hlednou vodn� hladinou");
			o.setVisible(true);
			o.setLocation(poziceX, poziceY);
			o.pack();
		} else if (e.getSource().equals(btn2)) {
			System.exit(0);
			animator.stop();
		}
	}
}
