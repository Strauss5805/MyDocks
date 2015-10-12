package de.unihamburg.informatik.wtm.docks;

import java.awt.Dimension; 
import java.awt.Graphics; 
import java.awt.Image; 

import javax.swing.ImageIcon; 
import javax.swing.JFrame; 
import javax.swing.JPanel; 
import javax.swing.JScrollPane;

class ImageImplement extends JPanel 
{ 
	private Image img; 
	public ImageImplement(Image img) 
	{ 
		this.img = img; 
		Dimension size = new Dimension(img.getWidth(null), img.getHeight(null)); 
		setPreferredSize(size); 
		setMinimumSize(size); 
		setMaximumSize(size); 
		setSize(size); 
		setLayout(null); 
	} 
	public void paintComponent(Graphics g) 
	{ 
		g.drawImage(img, 0, 0, null); 
	} 
} 
public class Visualizer extends JFrame 
{ 
	/**
	 * 
	 */
	private static final long serialVersionUID = 8020021520820097716L;
	public static void main(String args[]) 
	{ 
		//new Visualizer().start(); 
		String res_0 = "clean the table";
		String res_1 = "help";
		String res_2 = "please stop yourself";
		String res_3 = "clean this object";
		String res_4 = "clean this banana";
		String res_5 = "clean the table";
		
		
	} 
	public void start() 
	{ 
		ImageImplement panel = new ImageImplement(new ImageIcon("graph.png").getImage()); 
		JScrollPane scrPane = new JScrollPane(panel);
		add(scrPane); 
		//add(panel); 
		setVisible(true); 
		setSize(panel.getWidth(),1080); 
		setDefaultCloseOperation(EXIT_ON_CLOSE); 
		//pack();
		
	} 
}

