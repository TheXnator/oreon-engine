package simulations.templates;

import static org.lwjgl.opengl.GL11.GL_CCW;
import static org.lwjgl.opengl.GL11.GL_CW;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_FUNC;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.GL_COMPARE_REF_TO_TEXTURE;
import static org.lwjgl.opengl.GL30.GL_DEPTH_COMPONENT32F;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;

import java.nio.ByteBuffer;

import modules.sky.SkySphere;
import modules.terrain.Terrain;
import modules.water.Water;
import engine.configs.RenderingConfig;
import engine.core.Constants;
import engine.core.Texture;
import engine.core.OpenGLWindow;
import engine.main.RenderingEngine;

public class TerrainSimulation extends Simulation{

	private SkySphere skySphere;
	private Terrain terrain;
	private Water water;
	
	public void init()
	{
		super.init();
		
		setSceneTexture(new Texture());
		getSceneTexture().generate();
		getSceneTexture().bind();
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, OpenGLWindow.getWidth(), OpenGLWindow.getHeight(), 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		setSceneDepthmap(new Texture());
		getSceneDepthmap().generate();
		getSceneDepthmap().bind();
		glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, OpenGLWindow.getWidth(), OpenGLWindow.getHeight(), 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		getSceneFBO().bind();
		getSceneFBO().setDrawBuffer(0);
		getSceneFBO().colorTextureAttachment(getSceneTexture().getId(), 0);
		getSceneFBO().depthbufferAttachment(OpenGLWindow.getWidth(), OpenGLWindow.getHeight());
		getSceneFBO().depthTextureAttachment(getSceneDepthmap().getId());
		getSceneFBO().checkStatus();
		getSceneFBO().unbind();
		
		setSkySphere(new SkySphere());
	}

	public void update()
	{
		getRoot().input();
		getRoot().update();
		skySphere.update();
		if (water != null)
			water.update();
		if (terrain != null)
			terrain.update();
	}
	
	public void render()
	{
		if (water != null)
		{
			//water.renderFFT();
	
			RenderingEngine.setClipplane(water.getClipplane());
			
			//mirror scene to clipplane
			
			getRoot().getTransform().setScaling(1,-1,1);
			// prevent refelction distortion overlap
			skySphere.getTransform().setScaling(1.1f,-1,1.1f);
			skySphere.getTransform().getTranslation().setY(RenderingEngine.getClipplane().getW() - 
					(skySphere.getTransform().getTranslation().getY() - RenderingEngine.getClipplane().getW()));
			
			synchronized(Terrain.getLock()){
				if (terrain != null){
					terrain.getTerrainConfiguration().setScaleY(terrain.getTerrainConfiguration().getScaleY() * -1f);
					terrain.getTransform().getLocalTranslation().setY(RenderingEngine.getClipplane().getW() - 
							(terrain.getTransform().getLocalTranslation().getY() - RenderingEngine.getClipplane().getW()));
				}
				update();
			
			
				//render reflection to texture

				glViewport(0,0,OpenGLWindow.getWidth()/2, OpenGLWindow.getHeight()/2);
			
				water.getReflectionFBO().bind();
				RenderingConfig.clearScreenDeepOceanReflection();
				glFrontFace(GL_CCW);
				getRoot().render();
				//skySphere.render();
				if (terrain != null){
					terrain.render();
				}
				glFinish(); //important, prevent conflicts with following compute shaders
				glFrontFace(GL_CW);
				water.getReflectionFBO().unbind();
			
				// antimirror scene to clipplane
		
				getRoot().getTransform().setScaling(1,1,1);
				skySphere.getTransform().setScaling(1,1,1);
				skySphere.getTransform().getTranslation().setY(RenderingEngine.getClipplane().getW() + 
				(RenderingEngine.getClipplane().getW() - skySphere.getTransform().getTranslation().getY()));
				if (terrain != null){
					terrain.getTerrainConfiguration().setScaleY(terrain.getTerrainConfiguration().getScaleY()/ -1f);
					terrain.getTransform().getLocalTranslation().setY(RenderingEngine.getClipplane().getW() + 
							(RenderingEngine.getClipplane().getW() - terrain.getTransform().getLocalTranslation().getY()));
				}
				update();
			
				// render to refraction texture
			
				water.getRefractionFBO().bind();
				RenderingConfig.clearScreenDeepOceanRefraction();
				getRoot().render();
				if (terrain != null){
					terrain.render();
				}
				glFinish(); //important, prevent conflicts with following compute shaders
				water.getRefractionFBO().unbind();
			}
			
			RenderingEngine.setClipplane(Constants.PLANE0);	
		}
	
		glViewport(0,0,OpenGLWindow.getWidth(), OpenGLWindow.getHeight());
		getSceneFBO().bind();
		RenderingConfig.clearScreen();	
		if (water != null){
			water.render();
		}
		if (terrain != null) {
			terrain.render();
		}
		if (!RenderingEngine.isGrid())
			skySphere.render();
		getRoot().render();
		glFinish(); //important, prevent conflicts with following compute shaders
		getSceneFBO().unbind();
	}

	public Water getWater() {
		return water;
	}

	public void setWater(Water water) {
		this.water = water;
	}

	public Terrain getTerrain() {
		return terrain;
	}

	public void setTerrain(Terrain terrain) {
		this.terrain = terrain;
	}

	public SkySphere getSkySphere() {
		return skySphere;
	}

	public void setSkySphere(SkySphere skyShape) {
		this.skySphere = skyShape;
	}

}
