package org.oreon.gl.components.gpgpu.fft;

import org.oreon.core.gl.pipeline.GLShaderProgram;
import org.oreon.core.util.ResourceLoader;

public class FFTTwiddleFactorsShader extends GLShaderProgram{

	private static FFTTwiddleFactorsShader instance = null;
	
	public static FFTTwiddleFactorsShader getInstance() 
	{
	    if(instance == null) 
	    {
	    	instance = new FFTTwiddleFactorsShader();
	    }
	      return instance;
	}
	
	protected FFTTwiddleFactorsShader()
	{
		super();
		
		addComputeShader(ResourceLoader.loadShader("shaders/computing/FastFourierTransform/TwiddleFactors.glsl"));
		compileShader();
		
		addUniform("N");
	}
	

	public void updateUniforms(int N)
	{
		setUniformi("N", N);
	}
}
