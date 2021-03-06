package org.oreon.core.vk.core.command;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_SHADER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBufferToImage;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkOffset3D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.oreon.core.vk.core.pipeline.VkPipeline;
import org.oreon.core.vk.core.util.VkUtil;

import lombok.Getter;

public class CommandBuffer {

	@Getter
	private VkCommandBuffer handle;
	@Getter 
	private PointerBuffer pHandle;
	
	private VkDevice device;
	private long commandPool;
	
	private VkRenderPassBeginInfo renderPassBeginInfo;
	
	public CommandBuffer(VkDevice device, long commandPool) {
		
		this.device = device;
		this.commandPool = commandPool;
		
		VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
	                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
	                .commandPool(commandPool)
	                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
	                .commandBufferCount(1);
		 
		pHandle = memAllocPointer(1);
		int err = vkAllocateCommandBuffers(device, cmdBufAllocateInfo, pHandle);
		
		if (err != VK_SUCCESS) {
		    throw new AssertionError("Failed to allocate command buffer: " + VkUtil.translateVulkanResult(err));
		}
		
		handle = new VkCommandBuffer(pHandle.get(0), device);
		
		cmdBufAllocateInfo.free();
	}
	
	public void beginRecord(int flags){
		
		VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .pNext(0)
                .flags(flags);
			
		int err = vkBeginCommandBuffer(handle, beginInfo);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to begin record command buffer: " + VkUtil.translateVulkanResult(err));
		}
        
        beginInfo.free();
	}
	
	public void finishRecord(){
			
		int err = vkEndCommandBuffer(handle);
		
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to finish record command buffer: " + VkUtil.translateVulkanResult(err));
        }
	}
	
	public void beginRenderPassCmd(long pipeline, long renderPass, int attachments){
		
		VkClearValue.Buffer clearValues = VkClearValue.calloc(attachments);
		
		for (int i=0; i<attachments; i++){
			clearValues.put(VkUtil.getBlackClearValues());
		}
		clearValues.flip();
		
		renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
				.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
				.pNext(0)
				.renderPass(renderPass)
				.pClearValues(clearValues);
		
		vkCmdBeginRenderPass(handle, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
		vkCmdBindPipeline(handle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
	}
	
	public void setViewPortCmd(){
		
		// TODO
	}
	
	public void setScissorCmd(){
		
		// TODO
	}
	
	public void pushConstantsCmd(long pipelineLayout, int stageFlags, ByteBuffer data){
		
		vkCmdPushConstants(handle,
				pipelineLayout,
				stageFlags,
				0,
				data);
	}
	
	public void recordIndexedRenderCmd(VkPipeline pipeline, long renderPass, long vertexBuffer,
			long indexBuffer, int indexCount, long[] descriptorSets, int width, int height,
			long framebuffer, int attachments){

		VkClearValue.Buffer clearValues = VkClearValue.calloc(attachments);
		
		for (int i=0; i<attachments; i++){
			clearValues.put(VkUtil.getBlackClearValues());
		}
		clearValues.flip();
		
		VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
					.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
					.pNext(0)
					.renderPass(renderPass)
					.pClearValues(clearValues);
			
		VkRect2D renderArea = renderPassBeginInfo.renderArea();
		renderArea.offset().set(0, 0);
		renderArea.extent().set(width, height);
		
		renderPassBeginInfo.framebuffer(framebuffer);
		
		vkCmdBeginRenderPass(handle, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
		vkCmdBindPipeline(handle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());
		
		LongBuffer offsets = memAllocLong(1);
		offsets.put(0, 0L);
		LongBuffer pVertexBuffers = memAllocLong(1);
		pVertexBuffers.put(0, vertexBuffer);

		vkCmdBindVertexBuffers(handle, 0, pVertexBuffers, offsets);
		vkCmdBindIndexBuffer(handle, indexBuffer, 0, VK_INDEX_TYPE_UINT32);
		vkCmdBindDescriptorSets(handle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayoutHandle(),
								0, descriptorSets, null);
		
		vkCmdDrawIndexed(handle, indexCount, 1, 0, 0, 0);
		vkCmdEndRenderPass(handle);
		
		memFree(pVertexBuffers);
		memFree(offsets);
		renderPassBeginInfo.free();
	}
	
	public void recordCopyBufferCmd(long srcBuffer, long dstBuffer,
								    long srcOffset, long dstOffset,
								    long size){
		
		VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1)
					.srcOffset(srcOffset)
					.dstOffset(dstOffset)
					.size(size);
		
		vkCmdCopyBuffer(handle, srcBuffer, dstBuffer, copyRegion);
	}
	
	public void recordCopyBufferToImageCmd(long srcBuffer, long dstImage, int width, int height, int depth){
		
		VkBufferImageCopy.Buffer copyRegion = VkBufferImageCopy.calloc(1)
					.bufferOffset(0)
					.bufferRowLength(0)
					.bufferImageHeight(0);
		
		VkImageSubresourceLayers subresource = VkImageSubresourceLayers.calloc()
					.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
					.mipLevel(0)
					.baseArrayLayer(0)
					.layerCount(1);
		
		VkExtent3D extent = VkExtent3D.calloc()
					.width(width)
					.height(height)
					.depth(depth);
		
		VkOffset3D offset = VkOffset3D.calloc()
					.x(0)
					.y(0)
					.z(0);
		
		copyRegion.imageSubresource(subresource);
		copyRegion.imageExtent(extent);
		copyRegion.imageOffset(offset);
	
		vkCmdCopyBufferToImage(handle, srcBuffer, dstImage,
							   VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegion);
	}
	
	public void recordImageLayoutTransitionCmd(long image, int oldLayout, int newLayout){

		int srcStageMask = 0;
		int dstStageMask = 0;
		int srcAccessMask = 0; 
		int dstAccessMask = 0;

		if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
			
		    srcAccessMask = 0;
		    dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;

		    srcStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
		    dstStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT;
		    
		} else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
			
		    srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
		    dstAccessMask = VK_ACCESS_SHADER_READ_BIT;

		    srcStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT;
		    dstStageMask = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
		    
		}
		
		VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1)
				.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
				.oldLayout(oldLayout)
				.newLayout(newLayout)
				.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
				.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
				.image(image)
				.srcAccessMask(srcAccessMask)
				.dstAccessMask(dstAccessMask);
		
		VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc()
				.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
				.baseMipLevel(0)
				.levelCount(1)
				.baseArrayLayer(0)
				.layerCount(1);
		
		barrier.subresourceRange(subresourceRange);
	
		vkCmdPipelineBarrier(handle,srcStageMask,dstStageMask,0,null,null,barrier);
	}
	
	public void destroy(){
		
		vkFreeCommandBuffers(device, commandPool, pHandle);
	}
	
}
