package model;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import app.OstrovApp;

import com.sun.opengl.util.Animator;
import com.sun.opengl.util.GLUT;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;


public class Renderer implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {
GL gl;
GLU glu;
GLUT glut;
Animator animator = new Animator();
GLAutoDrawable glDrawable;
GLUquadric quadric;

public boolean blend = true;
boolean textura = true;
boolean light = true;
boolean fly = true; 
boolean vlneni = false;
boolean renderovaciMod = true;
boolean keys[] = new boolean[256];

//nastavení promìnných pro výškové mapování a procházení v cyklu
int velikostMapy = 1024;
int velikostPodvodou = 1024;
int velikostVodnihoKroku = 16;
int VelikostTerenihoKrok = 8;
int velikostVody = 128;
int VelikostPodvodnihoKroku = 32;
int VertexShader;
int FragmentShader;
int width, height, dx, dy, ox, oy;

float vyskoveMeritko = 1.9f;
float meritkoVody = 0.60f;
float meritkoOstrova = 0.14f;
float vyska = 0.05f;
float vyskaPodvodou = 0.65f;
float meritkoPodvodou = 0.7f;
float yRotace;
float nahoruDolu = 0.0f;
float dopravaDoleva; 
float posX = 0;
float posY = 0;
float posZ = 0;
float step;
float time=0;
private int dlObject;
float[][] vertexes = new float[4][3]; // dvourozmìrné pole,které drží informace o ètyøech sadách vektorù
float[] normal = new float[3]; // pole k uložení normálových vektorù
float[] barvaMlhy = { 0.8f, 0.8f, 0.8f, 1.0f};

static final int x = 0;  
static final int y = 1;  
static final int z = 2; 

byte gVyskovaPodvodou[] = new byte[velikostPodvodou*velikostPodvodou];
byte gVyskovaMapa[] = new byte[velikostMapy*velikostMapy];
byte gVyskovaVoda[] = new byte[velikostVody*velikostVody];

Texture textur[] = new Texture[15]; //pole pro uložení naètených textur

long oldmils = System.currentTimeMillis();

	@Override
	public void init(GLAutoDrawable drawable) {
		glDrawable = drawable;
		glu = new GLU();
		gl = drawable.getGL();
		glut = new GLUT();
		
		//drawable.setGL(new DebugGL(drawable.getGL()));
		
		gl.glViewport(0, 0, width, height);
		gl.glShadeModel(gl.GL_SMOOTH);
		gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
		gl.glClearDepth(1.0f);
		gl.glEnable( GL.GL_AUTO_NORMAL );
		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glDepthFunc(gl.GL_LEQUAL);
						
		gl.glHint(gl.GL_PERSPECTIVE_CORRECTION_HINT, gl.GL_NICEST);
		glDrawable.addKeyListener(this);
		glDrawable.addMouseListener(this);
		glDrawable.addMouseMotionListener(this);
			
		NahrajRawSoubor("Data/terrain1.raw", velikostMapy * velikostMapy, gVyskovaMapa);//soubor s terenem ostrova
		NahrajRawSoubor("Data/terrain.raw", velikostPodvodou * velikostPodvodou, gVyskovaPodvodou);// soubor s terenem podvodou
		NahrajRawSoubor("Data/vodnihladina.raw", velikostVody*velikostVody, gVyskovaVoda);// soubor s modelem vodni hladiny
	
		//nacitani textur do pole
		pridejTexturu();
		initDisplayList(gl);
		//vytvoreni efektu mlhy, ktery je videt pouze v pozadi
		gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
	    gl.glFogfv(GL.GL_FOG_COLOR, barvaMlhy,0);
	    gl.glFogf(GL.GL_FOG_DENSITY, 0.75f);
	    gl.glHint(GL.GL_FOG_HINT, GL.GL_DONT_CARE);
	    gl.glFogf(GL.GL_FOG_START, 1.0f);
	    gl.glFogf(GL.GL_FOG_END, 7800.0f);
	    gl.glEnable(GL.GL_FOG);
	   /*
	    try {
			pripojShadery(gl);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	     */
		animator = new Animator(drawable);
		animator.start();
		animator.setRunAsFastAsPossible(true);
	}
	
	private void initDisplayList(GL gl){
		dlObject = gl.glGenLists(1);
		gl.glNewList(dlObject, gl.GL_COMPILE);
		rendrujVyskovaMapa(gVyskovaMapa);
		gl.glEndList();
		
		
	}
	
	private void pripojShadery(GL gl) throws Exception{
		//pokus o shadery, ktere se mi nepodarilo dodelat
		BufferedReader vertex_reader;
		
		VertexShader = gl.glCreateShader(gl.GL_VERTEX_SHADER);
		FragmentShader = gl.glCreateShader(gl.GL_FRAGMENT_SHADER);
		String line;
		String[] vertexString = new String[1];
		 		vertexString[0] = "";
				vertexString[1] = "\n";
				
		
		gl.glShaderSource(VertexShader, 1, vertexString, null, 0);
		gl.glCompileShader(VertexShader);
		String[] fragmentString = new String[1];
		fragmentString[0] = "";
		fragmentString[1] = "\n";
				
		gl.glShaderSource(FragmentShader, 1, fragmentString, null, 0);
		gl.glCompileShader(FragmentShader);
		int shader = gl.glCreateProgram();
		gl.glAttachShader(shader, VertexShader);
		gl.glAttachShader(shader, FragmentShader);
		gl.glLinkProgram(shader);
		gl.glValidateProgram(shader);
		gl.glUseProgram(shader);
	}
	

	public void pridejOsvetleni() {
		//pridani a nastaveni osvetleni
		float zluteAmbientni[] = {0.5f, 0.5f, 0.3f, 1.0f};
        float bileDifuzni[] = {1.0f, 1.0f, 1.0f, 0.0f};
        float pozice[] = {1000.0f, 1000.0f, 1000.0f, 1.0f};
        //gl.glLightModeli(gl.GL_LIGHT_MODEL_LOCAL_VIEWER, gl.GL_TRUE);
        float local_view[] =
        { 0.0f };
        gl.glLightfv(gl.GL_LIGHT0, gl.GL_AMBIENT, zluteAmbientni, 0);
        gl.glLightfv(gl.GL_LIGHT0, gl.GL_DIFFUSE, bileDifuzni, 0);
        gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, pozice, 0);
        gl.glLightModelfv(gl.GL_LIGHT_MODEL_AMBIENT, zluteAmbientni, 0);
        gl.glLightModelfv(gl.GL_LIGHT_MODEL_LOCAL_VIEWER, local_view, 0);
        
        float[] mat_dif = new float[] {1,1,1,1};// nastaveni materialu
		float[] mat_spec = new float[] {0.3f,0.3f,0.0f,1};// nastaveni materialu
		float[] mat_amb = new float[] {0.3f,0.3f,0.3f,1};// nastaveni materialu
		//gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_amb, 0); 
		//gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_dif, 0); 
		//gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_spec, 0); 
		//gl.glMaterialfv(GL.GL_FRONT, GL.GL_EMISSION, mat, 0); 
		gl.glClearColor(0.0f, 0.1f, 0.1f, 0.0f);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		//nastaveni materialu
		float no_mat[] = { 0.0f, 0.0f, 0.0f, 1.0f };
		float mat_ambientni[] = { 0.8f, 0.8f, 0.2f, 1.0f };
		float mat_diffuse[] = { 0.1f, 0.5f, 0.8f, 1.0f };
		float mat_specular[] = { 1.0f, 1.0f, 1.0f, 1.0f };
		float no_shininess[] = { 0.0f };
		float low_shininess[] = { 5.0f };
		float high_shininess[] = { 100.0f };
		float mat_emission[] = { 0.3f, 0.2f, 0.2f, 0.0f };
		
		gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);        
	
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		gl.glRotatef(nahoruDolu, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(dopravaDoleva, 0.0f, 1.0f, 0.0f);
		
		//prida skybox, ve kterem je cely ostrov uzavren
		gl.glPushMatrix();
		//gl.glScalef(meritko, meritko, meritko);
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);
		gl.glRotatef(-45.0f, 0.0f, 1.0f, 0.0f);
		gl.glScalef(2.0f, 2.0f, 2.0f);
		renderujSkybox();
		gl.glPopMatrix();
		
		if (light) {
			gl.glEnable(GL.GL_LIGHTING);
			gl.glEnable(GL.GL_LIGHT0);
			

		} else {
			gl.glDisable(GL.GL_LIGHTING);
			gl.glDisable(GL.GL_LIGHT0);
		}
		
		if (fly){
			//rezim volne kamery - letani
			gl.glLoadIdentity();
			gl.glRotatef(nahoruDolu, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(dopravaDoleva, 0.0f, 1.0f, 0.0f);
			gl.glTranslatef(posX, posY, posZ);
			//nastaveni vychoziho pohledu, up vektor na ose y
			glu.gluLookAt(400, 60, 500, 250, 50, 250, 0, 1, 0);
		}else{
			//rezim pohledu z mola - bez chozeni
			gl.glLoadIdentity();
			//osetreni aby jsme se po navratu na volnou kameru, neposunuli nekam do ztracena
			posX = 0;
			posY = 0;
			posZ = 0;
			
			gl.glRotatef(nahoruDolu, 1.0f, 0.0f, 0.0f);
			gl.glRotatef(dopravaDoleva, 0.0f, 1.0f, 0.0f);
			//nastaveni pohledu z mola - vyhled bez chozeni, up vektor na ose y
			glu.gluLookAt(365, 30, 361, 500, 30, 500, 0, 1, 0);
		}
		
		
		pridejOsvetleni();
		gl.glEnable(GL.GL_NORMALIZE);
		long mils = System.currentTimeMillis();
		step = 360*(mils - oldmils)/12000.0f;
		float fps = 1000.0f/(mils-oldmils);
		oldmils = mils;
		
		
		System.out.println(fps + " krok:" + VelikostTerenihoKrok +" druhy:" + velikostVodnihoKroku +" step: " +step);
		
		if(renderovaciMod) 
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
		else 
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
			
		//prida molo
		gl.glPushMatrix();
		gl.glTranslatef(360.0f, 6.1f, 355.0f);
		gl.glRotatef(-30.0f, 0.0f, 1.0f, 0.0f);
		pridejMolo();
		gl.glPopMatrix();
		
	    gl.glEnable(gl.GL_CULL_FACE);
		gl.glCullFace(gl.GL_FRONT);
		
		//vyrenderuje ostrov
		gl.glPushMatrix();
		gl.glScalef(meritkoOstrova, meritkoOstrova * vyskoveMeritko, meritkoOstrova);
		gl.glTranslatef(1700.0f, 0.0f, 1700.0f);
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambientni, 0);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
		 
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
		   
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, low_shininess, 0);
		  
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_EMISSION, no_mat, 0);
		
		//gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glCallList(1);
		//rendrujVyskovaMapa(gVyskovaMapa);
		
		gl.glPopMatrix();
		gl.glDisable(gl.GL_CULL_FACE);
		
		//vyrenderuje ostrov
		gl.glPushMatrix();
		gl.glScalef(meritkoOstrova *2, meritkoOstrova * vyskoveMeritko, meritkoOstrova*2);
		gl.glTranslatef(0.0f, 0.0f, 0.0f);
		 
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambientni, 0);
		 
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, low_shininess, 0);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_EMISSION, no_mat, 0);
		//gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glCallList(1);
		//rendrujVyskovaMapa(gVyskovaMapa);
		
		gl.glPopMatrix();
		gl.glDisable(gl.GL_CULL_FACE);
		//vyrenderuje ostrov
		gl.glPushMatrix();
		gl.glScalef(meritkoOstrova * 2, meritkoOstrova * vyskoveMeritko, meritkoOstrova*3);
		gl.glTranslatef(900.0f, -30.0f, 000.0f);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambientni, 0);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, low_shininess, 0);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_EMISSION, no_mat, 0);
		//gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glCallList(1);
		//rendrujVyskovaMapa(gVyskovaMapa);
		
		gl.glPopMatrix();
		gl.glDisable(gl.GL_CULL_FACE);
		//vyrenderuje ostrov
		gl.glPushMatrix();
		gl.glScalef(meritkoOstrova *2, meritkoOstrova * vyskoveMeritko *2, meritkoOstrova*2);
		gl.glTranslatef(0.0f, -70.0f, 1500.0f);
	   
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambientni, 0);
	    gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
	    gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, no_mat, 0);
	    gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, no_shininess, 0);
	    gl.glMaterialfv(GL.GL_FRONT, GL.GL_EMISSION, mat_emission, 0);
		
	    //gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glCallList(1);
		//rendrujVyskovaMapa(gVyskovaMapa);
		
		gl.glPopMatrix();
		gl.glDisable(gl.GL_CULL_FACE);
		
		//vyrednderuje podklad pod vodou
	    gl.glEnable(gl.GL_CULL_FACE);
		gl.glCullFace(gl.GL_BACK);
		gl.glPushMatrix();
		gl.glTranslatef( 0.0f, -70.0f, 0.0f);
		gl.glScalef(meritkoPodvodou, meritkoPodvodou * vyskaPodvodou, meritkoPodvodou);
		renderujPodvodou(gVyskovaPodvodou);
		gl.glPopMatrix();
		gl.glDisable( GL.GL_TEXTURE_GEN_S);
	    gl.glDisable( GL.GL_TEXTURE_GEN_T);
	    gl.glDisable( GL.GL_TEXTURE_GEN_R);
	    		
		//prida vodu
		gl.glPushMatrix();
		gl.glTranslated(0.0f, 19.5f, 0.0f);

		gl.glScalef(meritkoVody, meritkoVody * vyska, meritkoVody);
	   
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambientni, 0);
	    gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
	    gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, no_mat, 0);
	    gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, high_shininess, 0);
	    gl.glMaterialfv(GL.GL_FRONT, GL.GL_EMISSION, mat_emission, 0);
		
	    if (blend) {
			gl.glEnable(gl.GL_BLEND);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); 
			gl.glBlendFunc(GL.GL_DST_COLOR, GL.GL_CONSTANT_ALPHA); 
		}
		renderujVodu(gVyskovaVoda);
		gl.glPopMatrix();
		gl.glDisable(gl.GL_BLEND);
		klavesnice();
		gl.glDisable(gl.GL_CULL_FACE);
		
	
	}
	
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	      if (height <= 0) // vyvarovani se deleni nulou
	          height = 1;
		
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(gl.GL_PROJECTION);
		gl.glLoadIdentity();
		
		glu.gluPerspective(45, width/(float)height, 0.1f, 10000.0f);
		gl.glMatrixMode(gl.GL_MODELVIEW);
		gl.glLoadIdentity();

	}
	
	@Override
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
			boolean deviceChanged) {
		// TODO Auto-generated method stub
	}
	   
	public void NahrajRawSoubor(String jmeno, int delka, byte[] pVyskovaMapa){
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(jmeno);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.getMessage();
		}
		
		try {
			fis.read(pVyskovaMapa, 0, delka);
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.getMessage();
		}
		
		for (int i = 0; i < pVyskovaMapa.length; i++) {
			pVyskovaMapa[i] &=0xff;
		}
	}

	public void zredukujNaJednotku(float[] vector) { 
		   //zredukuje normalovy vektor na jednotkovy normalovy vektor
		   float length = (float) Math.sqrt((vector[0] * vector[0]) + (vector[1] * 
	                vector[1]) + (vector[2] * vector[2]));

	        if (length == 0.0f)  
	            length = 1.0f; 

	        vector[0] /= length;  
	        vector[1] /= length;  
	        vector[2] /= length;  
	}
	   
    public void vypoctiNormalu(float[][] v, float[] out) {
    	  float[] v1 = new float[3];
          float[] v2 = new float[3];  
          
          v1[x] = v[0][x] - v[1][x];  
          v1[y] = v[0][y] - v[1][y]; 
          v1[z] = v[0][z] - v[1][z]; 
          
          v2[x] = v[1][x] - v[2][x];  
          v2[y] = v[1][y] - v[2][y];  
          v2[z] = v[1][z] - v[2][z];  
          
          out[x] = v1[y] * v2[z] - v1[z] * v2[y]; 
          out[y] = v1[z] * v2[x] - v1[x] * v2[z]; 
          out[z] = v1[x] * v2[y] - v1[y] * v2[x]; 

          zredukujNaJednotku(out); 

    }
	public int Height(byte[] pVyskovaMapa, int x, int y) {
		//vypocet vysky terenu
		x = x % velikostMapy;
		y = y % velikostMapy;
		return pVyskovaMapa[x + (y * velikostMapy)]&0xff;
	}
	
	public int Height2(byte[] pVyskovaMapa, int x, int y) {
		//vypocet vysky vody
		x = x % velikostVody;
		y = y % velikostVody;
		return pVyskovaMapa[x + (y * velikostVody)]&0xff;
	}
	    
	public void SetVertexColor(byte[] pHeightMap, int x, int y){   
		// nastavuje barvu terenu v zavislosti na vysce 
		int height = Height(pHeightMap, x, y);
  
            if(height >= 250)
                gl.glColor3f(0.984313f, 0.3019607f, 0.3019607f);
            else if(height > 200)
                gl.glColor3f(0.9f, 0.9607843f, 0.943147f);
            else if(height > 150)
            	gl.glColor3f(0.30196f, 1.0f, 0.50196f);
            else if(height > 105)
            	gl.glColor3f(0.011764f, 0.85294117f, 0.1490196f);
            else if(height > 99)
            	gl.glColor3f(0.984313f, 0.9019607f, 0.0f);
            else if(height > 50)
            	gl.glColor3f(0.984313f, 0.9019607f, 0.0f);
            else
               	gl.glColor3f(0.984313f, 0.9019607f, 0.0f);
        
	}
	
	public void SetVertexColor2(byte[] pHeightMap, int x, int y){   
		// nastavuje barvu vody v zavislosti na vysce ziskane z metody Height2 
		// a nasledne vydelene aby se veslo to intervalu od 0 do 1 a nakonec zasvetleni
		float fColor = (Height2(pHeightMap, x, y)/256.0f) + 0.22f;
		gl.glColor3f(0.1f, 0.7f, fColor);
	}
	
	
	public void renderujPodvodou(byte[] vyskovaPodvodou) {
		byte[] pVyskovaPodvodou = vyskovaPodvodou;
		
		if (textura == true) {
			 gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
			 gl.glEnable( GL.GL_TEXTURE_GEN_S);
		     gl.glEnable( GL.GL_TEXTURE_GEN_T);
		     gl.glEnable( GL.GL_TEXTURE_GEN_R);
		     textur[12].enable();
		     textur[12].bind();
		} 
	
		//gl.glBegin(gl.GL_LINES);
		//gl.glBegin(gl.GL_TRIANGLE_STRIP );
		gl.glBegin(gl.GL_QUADS);
		//gl.glBegin(gl.GL_TRIANGLES);
	
		for (int X = 0; X < (velikostPodvodou); X += VelikostPodvodnihoKroku) 
				for (int Y = 0; Y < (velikostPodvodou); Y+= VelikostPodvodnihoKroku) {
					
					int x = X;
					int y = Height(pVyskovaPodvodou, X, Y);
					int z = Y;
					if (textura == true) {
						 gl.glTexCoord2f(((float)x / (float)velikostPodvodou), ((float)z / (float)velikostPodvodou));
					} else {
						SetVertexColor(pVyskovaPodvodou, x, z);
					}
	                gl.glVertex3i(x, y, z); 
	               
	                x = X;
					y = Height(pVyskovaPodvodou, X, Y + VelikostPodvodnihoKroku );
					z = Y + VelikostPodvodnihoKroku;
					if (textura == true) {
						 gl.glTexCoord2f(((float)x / (float)velikostPodvodou), ((float)(z + 1) / (float)velikostPodvodou));
					} else {
						SetVertexColor(pVyskovaPodvodou, x, z);
					}
					gl.glVertex3i(x, y, z);
			        
					x = X + VelikostPodvodnihoKroku;
			        y = Height(pVyskovaPodvodou, X + VelikostPodvodnihoKroku, Y + VelikostPodvodnihoKroku );
			        z = Y + VelikostPodvodnihoKroku;
			        if (textura == true) {
			        	gl.glTexCoord2f(((float)(x + 1) / (float)velikostPodvodou), ((float)(z + 1) / (float)velikostPodvodou));
					} else {
						SetVertexColor(pVyskovaPodvodou, x, z);
					}
			        gl.glVertex3i(x, y, z);     
			        
	                x = X+ VelikostPodvodnihoKroku;
					y = Height(pVyskovaPodvodou, X+ VelikostPodvodnihoKroku, Y);
					z = Y;
					if (textura == true) {
						 gl.glTexCoord2f(((float)x / (float)velikostPodvodou), ((float)z / (float)velikostPodvodou));
					} else {
						SetVertexColor(pVyskovaPodvodou, x, z);
					}
	                gl.glVertex3i(x, y, z);    
				}
		
		gl.glEnd();
		gl.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);		
	}
	
	public void rendrujVyskovaMapa(byte[] vyskovaMapa) {
		byte[] pVyskovaMapa = vyskovaMapa;
		
		if (textura == true) {
			gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
			textur[6].enable();
			textur[6].bind();
		} 
		
		//gl.glBegin(gl.GL_LINES);
		gl.glBegin(gl.GL_TRIANGLE_STRIP );
		//gl.glBegin(gl.GL_QUADS);
		//gl.glBegin(gl.GL_TRIANGLES);
			
		gl.glEnable(GL.GL_NORMALIZE);
        for (int X = VelikostTerenihoKrok; X < (velikostMapy-VelikostTerenihoKrok); X += VelikostTerenihoKrok) 
			for (int Y = VelikostTerenihoKrok; Y < (velikostMapy-VelikostTerenihoKrok); Y+= VelikostTerenihoKrok) {
					
				int x = X;
				int y = Height(pVyskovaMapa, X, Y);
				int z = Y;
				if (textura == true) {
					 gl.glTexCoord2f(((float)x / (float)velikostMapy), ((float)z / (float)velikostMapy));
				} else {
					SetVertexColor(pVyskovaMapa, x, z);
				}
				vertexes[0][0] = x;  
	            vertexes[0][1] = y; 
	            vertexes[0][2] = z;  
	            normal[0]=Height(pVyskovaMapa, X-VelikostTerenihoKrok, Y)-Height(pVyskovaMapa, X+VelikostTerenihoKrok, Y);
		        normal[1]=Height(pVyskovaMapa, X, Y-VelikostTerenihoKrok)-Height(pVyskovaMapa, X, Y+VelikostTerenihoKrok);
	            normal[2]=2*VelikostTerenihoKrok;
	            
	            x = X + VelikostTerenihoKrok;
		        y = Height(pVyskovaMapa, X + VelikostTerenihoKrok, Y );
		        z = Y;
		        vertexes[1][0] = x;  
	            vertexes[1][1] = y;  
	            vertexes[1][2] = z; 
	            
	            gl.glNormal3f(normal[0], normal[1], normal[2]);  
	            gl.glVertex3f(vertexes[0][0], vertexes[0][1], vertexes[0][2]);
              
	            if (textura == true) {
		        	gl.glTexCoord2f(((float)(x) / (float)velikostMapy), ((float)z / (float)velikostMapy));
				} else {
					SetVertexColor(pVyskovaMapa, x, z);
				}
		        
	            gl.glVertex3f(vertexes[1][0], vertexes[1][1], vertexes[1][2]);
    		}
	
		gl.glEnd();
		gl.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
	
	}
	
	public void renderujVodu(byte[] vyskovaVoda) {
		byte[] pVyskovaMapa = vyskovaVoda;
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		textur[13].enable();
		textur[13].bind();
     
      time+=step;
      float offset=(float)Math.cos(time*Math.PI/70.0)*60;
      
		gl.glBegin(gl.GL_QUADS);
		 		for (int i = 0; i < 10; i++) 
		 			for (int j = 0; j < 10; j++) {
		 			for (int X = i * velikostVody; X < (velikostVody)+(i * velikostVody); X += velikostVodnihoKroku) 
		 				for (int Y = j * velikostVody; Y < (velikostVody) + (j * velikostVody); Y+= velikostVodnihoKroku) {
		 					
		 					int x = X;
		 					int z = Y;
		 					int y = Height2(pVyskovaMapa, X, Y)+(int)(offset*Math.cos(x*Math.PI/20.0)*Math.cos(z*Math.PI/20.0));
		 					vertexes[0][0] = x;  
		 	                vertexes[0][1] = y; 
		 	                vertexes[0][2] = z; 
		 	                //gl.glTexCoord2f(((float)x / (float)velikostVody), ((float)z / (float)velikostVody));
		 					SetVertexColor2(pVyskovaMapa, x, z);
		 					gl.glVertex3f(vertexes[0][0], vertexes[0][1], vertexes[0][2]);
		 	              
		 									
		 					x = X;
		 					z = Y + velikostVodnihoKroku;
		 					y = Height2(pVyskovaMapa, X, Y + velikostVodnihoKroku )+(int)(offset*Math.cos(x*Math.PI/20.0)*Math.cos(z*Math.PI/20.0));
		 					
		 					vertexes[1][0] = x; 
			 	            vertexes[1][1] = y;  
			 	            vertexes[1][2] = z; 
			 	           //gl.glTexCoord2f(((float)x / (float)velikostVody), ((float)(z + 1) / (float)velikostVody));  
			 	            SetVertexColor2(pVyskovaMapa, x, z);
			 	            gl.glVertex3f(vertexes[1][0], vertexes[1][1], vertexes[1][2]);
		 					x = X + velikostVodnihoKroku;
		 					z = Y + velikostVodnihoKroku;
			 			    y = Height2(pVyskovaMapa, X + velikostVodnihoKroku, Y + velikostVodnihoKroku )+(int)(offset*Math.cos(x*Math.PI/20.0)*Math.cos(z*Math.PI/20.0));
		 			        vertexes[2][0] = x; 
		 	                vertexes[2][1] = y;
		 	                vertexes[2][2] = z; 
							
		 	               //gl.glTexCoord2f(((float)(x + 1) / (float)velikostVody), ((float)(z + 1) / (float)velikostVody));
		 	                SetVertexColor2(pVyskovaMapa, x, z);
		 	                gl.glVertex3f(vertexes[2][0], vertexes[2][1], vertexes[2][2]);
						  
		 			         
		 			        x = X + velikostVodnihoKroku;
		 			        z = Y;
		 			        y = Height2(pVyskovaMapa, X + velikostVodnihoKroku, Y )+(int)(offset*Math.cos(x*Math.PI/20.0)*Math.cos(z*Math.PI/20.0));
		 			        vertexes[3][0] = x;  
			                vertexes[3][1] = y; 
			                vertexes[3][2] = z; 
			                //gl.glTexCoord2f(((float)x / (float)velikostVody), ((float)z / (float)velikostVody));
			                SetVertexColor2(pVyskovaMapa, x, z);
			                gl.glVertex3f(vertexes[3][0], vertexes[3][1], vertexes[3][2]);
				          
			                vypoctiNormalu(vertexes, normal);  
 				            gl.glNormal3f(normal[0], normal[1], normal[2]);  

		 					}
		 				}
		 		
		gl.glEnd();
		gl.glColor4f(1.0f, 1.0f, 1.0f, 0.5f); 
		
	}


	public void renderujSkybox() {
		//gl.glDisable(gl.GL_LIGHTING);
		//predni strana
		
		textur[0].enable(); 
		textur[0].bind();

		gl.glBegin(gl.GL_QUADS);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3i( -2000, -400, -2000);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3i(-2000, 3600, -2000);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3i(-2000, 3600, 2000);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3i( -2000, -400, 2000);
		gl.glEnd();
		textur[0].disable(); 
		
		textur[1].enable(); 
		textur[1].bind();
		
		// zadni strana
	
		gl.glBegin(gl.GL_QUADS);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3i( 2000, -400, -2000);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3i(2000, 3600, -2000);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3i(2000, 3600, 2000);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3i( 2000, -400, 2000);
		gl.glEnd();
		textur[1].disable(); 
				
		// vrsek
		textur[5].enable(); 
		textur[5].bind();

		gl.glBegin(gl.GL_QUADS);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3i( -2000, 3600, -2000);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3i(2000, 3600, -2000);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3i(2000, 3600, 2000);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3i( -2000, 3600, 2000);
		gl.glEnd();
		textur[5].disable(); 
		
		// prava
		textur[3].enable();
		textur[3].bind();
	
		gl.glBegin(gl.GL_QUADS);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3i( -2000, 3600, -2000);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3i(-2000, -400, -2000);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3i(2000, -400, -2000);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3i(2000, 3600, -2000);
		gl.glEnd();
		textur[3].disable(); 
		// leva
		textur[2].enable(); 
		textur[2].bind();
		
		gl.glBegin(gl.GL_QUADS);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3i( -2000, 3600, 2000);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3i(-2000, -400, 2000);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3i(2000, -400, 2000);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3i(2000, 3600, 2000);
		gl.glEnd(); 
		
		textur[2].disable(); 
		//gl.glEnable(gl.GL_LIGHTING); 
		
		}

	public void pridejMolo() {
		//vrchni deska
		textur[8].enable();
		textur[8].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		//gl.glColor3f(1.0f, 0.0f, 1.0f);
		gl.glNormal3f(0.0f, 1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(0, 18.5f, 0);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 18.5f, 0);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.0f,18.5f, 6.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(0, 18.5f, 6.0f);
		gl.glEnd();
		textur[8].disable();
		
		//vrchni deska
		textur[8].enable();
		textur[8].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 1.0f, 0.0f);
		//gl.glColor3f(1.0f, 0.0f, 1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 18.5f, 0);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(16.0f, 18.5f, 0);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(16.0f, 18.5f, 6.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.0f, 18.5f, 6.0f);
		gl.glEnd();
		textur[8].disable();
		
		textur[8].enable();
		textur[8].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		//spodni deska
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, -1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(0, 18.0f, 0);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 18.0f, 0);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.0f,18.0f, 6.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(0, 18.0f, 6.0f);
		gl.glEnd();
		textur[8].disable();
		
		textur[8].enable();
		textur[8].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		//spodni deska
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, -1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 18.0f, 0);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(16.0f, 18.0f, 0);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(16.0f, 18.0f, 6.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.0f, 18.0f, 6.0f);
		gl.glEnd();
		textur[8].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		//bocni zadni prava deska
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, -1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(0, 18.0f, 0);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.0f, 18.0f, 0);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.0f, 18.5f, 0);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(0, 18.5f, 0);
		gl.glEnd();
		textur[9].disable();		
		
		//bocni zadni deska 
		textur[10].enable();
		textur[10].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(-1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(0, 18.0f, 0);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(0, 18.0f, 6.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(0, 18.5f, 6.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(0, 18.5f, 0);
		gl.glEnd();
		textur[9].disable();
		
		//bocni predni prava deska
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, -1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 18.0f, 0);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(16.0f, 18.0f, 0);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(16.0f, 18.5f, 0);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 18.5f, 0);
		gl.glEnd();
		textur[9].disable();
		
		//bocni predni leva deska
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 18.0f, 6.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(16.0f, 18.0f, 6.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(16.0f, 18.5f, 6.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 18.5f, 6.0f);
		gl.glEnd();
		textur[9].disable();
		
		//bocni zadni leva deska
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(0, 18.0f, 6.0f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.0f, 18.0f, 6.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.0f, 18.5f, 6.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(0, 18.5f, 6.0f);
		gl.glEnd();
		textur[9].disable();
		
		//bocni predni deska
		textur[10].enable();
		textur[10].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(16.0f, 18.0f, 0);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(16.0f, 18.0f, 6.0f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(16.0f, 18.5f, 6.0f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(16.0f, 18.5f, 0);
		gl.glEnd();
		textur[9].disable();
		
		//noha predni leva
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(-1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, 6.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, 5.7f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, 6.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, 6.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, 5.7f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, 6.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, -1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, 5.7f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, 5.7f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, 6.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, 6.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, 6.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, 6.3f);
		gl.glEnd();
		textur[9].disable();

		textur[11].enable();
		textur[11].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, -1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 10.0f, 6.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 10.0f, 6.3f);
		gl.glEnd();
		textur[11].disable();
		
		textur[11].enable();
		textur[11].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 19.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 19.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, 6.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, 6.3f);
		gl.glEnd();
		textur[11].disable();
				
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		//noha predni prava
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(-1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, 0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, -0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, 0.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, 0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, -0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, 0.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, -1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, -0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, -0.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, 0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, 0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, 0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, 0.3f);
		gl.glEnd();
		textur[9].disable();
				
		textur[11].enable();
		textur[11].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, -1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 10.0f, 0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 10.0f, 0.3f);
		gl.glEnd();
		textur[11].disable();
		
		textur[11].enable();
		textur[11].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(15.0f, 19.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(15.6f, 19.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(15.6f, 19.0f, 0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(15.0f, 19.0f, 0.3f);
		gl.glEnd();
		textur[11].disable();
		
		//noha zadni prava
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(-1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f, 0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f, -0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f, 0.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f, 0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f, -0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f, 0.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, -1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f, -0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f, -0.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f, 0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f, 0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f, 0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f, 0.3f);
		gl.glEnd();
		textur[9].disable();

		textur[11].enable();
		textur[11].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, -1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 10.0f, 0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 10.0f, 0.3f);
		gl.glEnd();
		textur[11].disable();
		
		textur[11].enable();
		textur[11].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 19.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 19.0f, -0.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f, 0.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f, 0.3f);
		gl.glEnd();
		textur[11].disable();

		//noha zadni leva
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(-1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f, 6.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f, 5.7f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f,6.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(1.0f, 0.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f,6.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f, 5.7f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f,6.3f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, -1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f, 5.7f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f, 5.7f);
		gl.glEnd();
		textur[9].disable();
		
		textur[9].enable();
		textur[9].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 0.0f, 1.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f,6.3f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f,6.3f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f,6.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f,6.3f);
		gl.glEnd();
		textur[9].disable();
				
		textur[11].enable();
		textur[11].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, -1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 10.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 10.0f,6.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 10.0f,6.3f);
		gl.glEnd();
		textur[11].disable();
		
		textur[11].enable();
		textur[11].bind();
		gl.glTexEnvi(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
		gl.glBegin(gl.GL_QUADS);
		gl.glNormal3f(0.0f, 1.0f, 0.0f);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex3f(8.0f, 19.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex3f(8.6f, 19.0f, 5.7f);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex3f(8.6f, 19.0f,6.3f);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex3f(8.0f, 19.0f,6.3f);
		gl.glEnd();		
		textur[11].disable();
		
	}
		 
	 public void pridejTexturu(){
		for (int i = 0; i < 14; i++) {
			 File file = new File("Data/"+i+".jpg");
			 try {System.out.println("Naèítám texturu...");
			 textur[i] = TextureIO.newTexture(file, false);
			 } catch (IOException e) {System.err.println("Chyba cteni souboru s texturou");
		}		 
			 
			 gl.glTexParameteri(gl.GL_TEXTURE_2D,gl.GL_TEXTURE_MIN_FILTER,gl.GL_LINEAR);// Lineární filtrování
			 gl.glTexParameteri(gl.GL_TEXTURE_2D,gl.GL_TEXTURE_MAG_FILTER,gl.GL_LINEAR);
			
			 gl.glTexParameteri( GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT );		   
			 gl.glTexParameteri( GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT );
			 gl.glTexParameteri( GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_R, GL.GL_REPEAT );
			
			

			/* gl.glTexGeni( GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_REFLECTION_MAP );
			 gl.glTexGeni( GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_REFLECTION_MAP );
			 gl.glTexGeni( GL.GL_R, GL.GL_TEXTURE_GEN_MODE, GL.GL_REFLECTION_MAP );
	       */

			 //glu.gluBuild2DMipmaps( GL.GL_TEXTURE_2D, GL.GL_RGB8, textur[i].getWidth(), textur[i].getHeight(), GL.GL_RGB, GL.GL_UNSIGNED_BYTE, data);
				
		}
	 }
	 
	 public void klavesnice(){
		 //metoda klavesnice spracovava pohyb, mohlo by byt v metode keyPressed,
		 //ale zde nedochazi k prvotnimu zaseknuti pri pohybu
		 float xrotrad,yrotrad;  
		 yrotrad = (float) (yRotace/180.0f*Math.PI);
		 xrotrad = (float) (nahoruDolu/180.0f*Math.PI);
		 
	    	if(keys[KeyEvent.VK_W]) { 
		    	posX += step * Math.sin(yrotrad);
		    	posZ += step * Math.cos(yrotrad);
		    	posY += step * Math.sin(xrotrad);
	           }
		    if(keys[KeyEvent.VK_S]) { 
		    	posX -= step * Math.sin(yrotrad);
		    	posZ -= step * Math.cos(yrotrad);
		    	posY -= step * Math.sin(xrotrad);
		      }
		    if (keys[KeyEvent.VK_A]) {
		    	posX += step * (float)Math.cos(yrotrad);
	            posZ -= step * (float)Math.sin(yrotrad);
	           
			}
		    if (keys[KeyEvent.VK_D]) {
		        posX -= step * (float)Math.cos(yrotrad);
	            posZ += step * (float)Math.sin(yrotrad);
			}	
	 }
	
	@Override
	public void keyPressed(KeyEvent e) {
		keys[e.getKeyCode()] = true;
		 
	    if(keys[KeyEvent.VK_ESCAPE]){
	      animator.stop();
	      System.exit(0);
	    }
	    if (keys[KeyEvent.VK_UP]) {
	    	VelikostTerenihoKrok -= 1;
		}
	    if (keys[KeyEvent.VK_DOWN]) {
	    	VelikostTerenihoKrok += 1;
		}
	    if (keys[KeyEvent.VK_LEFT]) {
	    	velikostVodnihoKroku -= 1;
		}
	    if (keys[KeyEvent.VK_RIGHT]) {
	    	velikostVodnihoKroku += 1;
		}
	    if (keys[KeyEvent.VK_T]) {
	    	  textura =!textura;
		}
	    if (keys[KeyEvent.VK_L]) {
	    	light = !light;
		}
	    if (keys[KeyEvent.VK_F]) {
	    	  fly = !fly;
		}
	    if (keys[KeyEvent.VK_R]) {
	    	renderovaciMod = !renderovaciMod;
		}
	    if (keys[KeyEvent.VK_V]) {
            vlneni =! vlneni;
           
            for (vyska = 0.035f;  vyska >= -0.035f ; vyska-= 0.005f)
             {
            	gl.glPushMatrix();
				gl.glTranslatef(0.0f, 16.0f, 0.0f);
				gl.glScalef(meritkoVody, meritkoVody * vyska, meritkoVody);
				renderujVodu(gVyskovaVoda);
				gl.glPopMatrix();
			}
		}	
	    if (keys[KeyEvent.VK_C]) {
            vlneni =! vlneni;

            for (vyska = -0.035f; vyska <= 0.035f; vyska += 0.005f) {
            	gl.glPushMatrix();
				gl.glTranslatef(0.0f, 20.0f, 0.0f);
				gl.glScalef(meritkoVody, meritkoVody * vyska, meritkoVody);
				renderujVodu(gVyskovaVoda);
				gl.glPopMatrix();
			}
		}	
	    if (keys[KeyEvent.VK_B]) {
			blend =! blend;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		keys[e.getKeyCode()] = false;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			ox = e.getX();
			oy = e.getY();
		}
	} 


	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseDragged(MouseEvent e) {
		
		dx = e.getX()-ox;
		dy = e.getY()-oy;
		ox = e.getX();
		oy = e.getY();
		
        
		nahoruDolu += (dy)/10.0f;
		if(nahoruDolu > 90)
			nahoruDolu = 90;
        if (nahoruDolu <=-90)
        	nahoruDolu = -90;
		yRotace -= dx/10.0f;
		dopravaDoleva = 360.0f - yRotace;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
	}
}

	
