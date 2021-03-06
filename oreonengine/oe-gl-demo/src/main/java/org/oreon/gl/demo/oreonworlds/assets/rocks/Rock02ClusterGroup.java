package org.oreon.gl.demo.oreonworlds.assets.rocks;

import java.util.ArrayList;
import java.util.List;

import org.oreon.core.gl.buffer.GLMeshVBO;
import org.oreon.core.gl.scenegraph.GLRenderInfo;
import org.oreon.core.gl.texture.GLTexture;
import org.oreon.core.gl.util.GLAssimpModelLoader;
import org.oreon.core.gl.wrapper.parameter.Default;
import org.oreon.core.instanced.InstancedObject;
import org.oreon.core.math.Vec3f;
import org.oreon.core.model.Model;
import org.oreon.core.scenegraph.NodeComponentKey;
import org.oreon.core.scenegraph.Renderable;
import org.oreon.core.util.Util;
import org.oreon.gl.demo.oreonworlds.shaders.assets.rocks.RockHighPolyShader;
import org.oreon.gl.demo.oreonworlds.shaders.assets.rocks.RockShadowShader;

public class Rock02ClusterGroup extends InstancedObject{

	public Rock02ClusterGroup(){
		
		List<Model<GLTexture>> models = GLAssimpModelLoader.loadModel("oreonworlds/assets/rocks/Rock_02","rock02.obj");
		
		List<Renderable> objects = new ArrayList<>();
		
		for (Model<GLTexture> model : models){
			
			GLMeshVBO meshBuffer = new GLMeshVBO();
			model.getMesh().setTangentSpace(true);
			Util.generateTangentsBitangents(model.getMesh());
			model.getMesh().setInstanced(true);
			
			meshBuffer.addData(model.getMesh());

			GLRenderInfo renderInfo = new GLRenderInfo(RockHighPolyShader.getInstance(), new Default(), meshBuffer);
			GLRenderInfo shadowRenderInfo = new GLRenderInfo(RockShadowShader.getInstance(), new Default(), meshBuffer);
	
			Renderable object = new Renderable();
			object.addComponent(NodeComponentKey.MAIN_RENDERINFO, renderInfo);
			object.addComponent(NodeComponentKey.SHADOW_RENDERINFO, shadowRenderInfo);
			object.addComponent(NodeComponentKey.MATERIAL0, model.getMaterial());
			objects.add(object);
		}
		
		addChild(new Rock02Cluster(10,new Vec3f(954,0,-30),objects));
	}

	public void run() {}
}
