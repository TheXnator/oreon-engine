package org.oreon.gl.components.filter.dofblur;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import org.oreon.core.context.EngineContext;
import org.oreon.core.gl.pipeline.GLShaderProgram;
import org.oreon.core.gl.texture.GLTexture;
import org.oreon.core.util.ResourceLoader;

public class DepthOfFieldVerticalBlurShader extends GLShaderProgram{

private static DepthOfFieldVerticalBlurShader instance = null;
	
	public static DepthOfFieldVerticalBlurShader getInstance() 
	{
	    if(instance == null) 
	    {
	    	instance = new DepthOfFieldVerticalBlurShader();
	    }
	      return instance;
	}
	
	protected DepthOfFieldVerticalBlurShader()
	{
		super();
		
		addComputeShader(ResourceLoader.loadShader("shaders/computing/DepthOfFieldBlur/verticalGaussianDoF_CS.glsl"));
		
		compileShader();
		
		addUniform("depthmap");
		addUniform("windowWidth");
		addUniform("windowHeight");
	}
	
	public void updateUniforms(GLTexture depthmap)
	{
		glActiveTexture(GL_TEXTURE0);
		depthmap.bind();
		setUniformi("depthmap", 0);
		setUniformf("windowWidth", EngineContext.getWindow().getWidth());
		setUniformf("windowHeight", EngineContext.getWindow().getHeight());
	}
}
