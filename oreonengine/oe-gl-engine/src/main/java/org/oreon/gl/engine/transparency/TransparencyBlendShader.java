package org.oreon.gl.engine.transparency;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.GL_TEXTURE3;
import static org.lwjgl.opengl.GL13.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13.GL_TEXTURE5;
import static org.lwjgl.opengl.GL13.GL_TEXTURE6;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import org.oreon.core.context.EngineContext;
import org.oreon.core.gl.pipeline.GLShaderProgram;
import org.oreon.core.gl.texture.GLTexture;
import org.oreon.core.util.ResourceLoader;

public class TransparencyBlendShader extends GLShaderProgram{

	private static TransparencyBlendShader instance = null;
	
	public static TransparencyBlendShader getInstance() 
	{
		if(instance == null) 
		{
			instance = new TransparencyBlendShader();
		}
		return instance;
	}
	
	protected TransparencyBlendShader() {
		
		super();
		
		addVertexShader(ResourceLoader.loadShader("shaders/transparencyBlend_VS.glsl"));
		addFragmentShader(ResourceLoader.loadShader("shaders/transparencyBlend_FS.glsl"));
		compileShader();
		
		addUniform("opaqueSceneTexture");
		addUniform("opaqueSceneLightScatteringTexture");
		addUniform("opaqueSceneDepthMap");
		addUniform("transparencyLayer");
		addUniform("transparencyLayerDepthMap");
		addUniform("transparencyAlphaMap");
		addUniform("transparencyLayerLightScatteringTexture");
		addUniform("width");
		addUniform("height");
	}
	
	public void updateUniforms(GLTexture opaqueSceneTexture, GLTexture opaqueSceneDepthMap,
			GLTexture opaqueSceneLightScatteringTexture,
			GLTexture transparencyLayer, GLTexture transparencyLayerDepthMap,
			GLTexture alphaMap, GLTexture transparencyLayerLightScatteringTexture)
	{
		setUniformi("width", EngineContext.getWindow().getWidth());
		setUniformi("height", EngineContext.getWindow().getHeight());
		
		glActiveTexture(GL_TEXTURE0);
		opaqueSceneTexture.bind();
		setUniformi("opaqueSceneTexture", 0);
		
		glActiveTexture(GL_TEXTURE1);
		opaqueSceneDepthMap.bind();
		setUniformi("opaqueSceneDepthMap", 1);
		
		glActiveTexture(GL_TEXTURE2);
		opaqueSceneLightScatteringTexture.bind();
		setUniformi("opaqueSceneLightScatteringTexture", 2);

		glActiveTexture(GL_TEXTURE3);
		transparencyLayer.bind();
		setUniformi("transparencyLayer", 3);
		
		glActiveTexture(GL_TEXTURE4);
		transparencyLayerDepthMap.bind();
		setUniformi("transparencyLayerDepthMap", 4);

		glActiveTexture(GL_TEXTURE5);
		alphaMap.bind();
		setUniformi("transparencyAlphaMap", 5);
		
		glActiveTexture(GL_TEXTURE6);
		transparencyLayerLightScatteringTexture.bind();
		setUniformi("transparencyLayerLightScatteringTexture", 6);
	}
}
