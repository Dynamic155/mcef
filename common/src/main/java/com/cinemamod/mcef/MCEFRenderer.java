/*
 *     MCEF (Minecraft Chromium Embedded Framework)
 *     Copyright (C) 2023 CinemaMod Group
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package com.cinemamod.mcef;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.UUID;

public class MCEFRenderer {
    private final boolean transparent;
    private GpuTexture texture;
    private GpuTextureView textureView;
    private int textureWidth = 0;
    private int textureHeight = 0;

    // Identifier for this renderer's texture
    private final Identifier textureLocation;
    private MCEFDirectTexture directTexture;
    private boolean textureRegistered = false;

    // Reusable native scratch buffer. CEF hands us BGRA8 frame data that may be a sub-rectangle
    // of a larger, strided source buffer; GpuDevice#writeToTexture requires a tightly-packed RGBA8
    // region with no stride/skip support, so every paint gets repacked into this buffer first.
    private ByteBuffer scratch;

    protected MCEFRenderer(boolean transparent) {
        this.transparent = transparent;
        // Generate a unique Identifier for this renderer
        String uniqueId = UUID.randomUUID().toString().toLowerCase().replace("-", "");
        this.textureLocation = Identifier.fromNamespaceAndPath("mcef", "browser_" + uniqueId);
    }

    public void initialize() {
        // Create and register the direct texture wrapper with Minecraft's TextureManager
        directTexture = new MCEFDirectTexture();
        Minecraft.getInstance().getTextureManager().register(textureLocation, directTexture);
        textureRegistered = true;
    }

    public GpuTexture getTexture() {
        return texture;
    }

    /**
     * Gets the Identifier that can be used with GuiGraphics and other Minecraft rendering methods.
     * This Identifier is registered with the TextureManager and points to the browser's texture.
     */
    public Identifier getTextureLocation() {
        return textureLocation;
    }

    /**
     * Check if the texture is ready for rendering with GuiGraphics
     */
    public boolean isTextureReady() {
        return texture != null && textureRegistered && directTexture != null;
    }

    public int getTextureID() {
        // For compatibility, return the OpenGL ID if texture exists
        if (texture instanceof GlTexture glTexture) {
            return glTexture.glId();
        }
        return 0;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public boolean isTransparent() {
        return transparent;
    }

    protected void cleanup() {
        if (texture != null) {
            texture.close();
            texture = null;
        }
        if (textureView != null) {
            textureView.close();
            textureView = null;
        }

        // Unregister from TextureManager
        if (textureRegistered && textureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(textureLocation);
            textureRegistered = false;
        }

        if (scratch != null) {
            MemoryUtil.memFree(scratch);
            scratch = null;
        }
    }

    /**
     * Full-frame paint. (Re)creates the backing texture if the size changed.
     */
    protected void onPaint(ByteBuffer buffer, int width, int height) {
        if (texture == null || textureWidth != width || textureHeight != height) {
            if (texture != null) texture.close();
            if (textureView != null) textureView.close();

            GpuDevice device = RenderSystem.getDevice();
            String label = "MCEF Browser Texture " + width + "x" + height;
            texture = device.createTexture(
                    label,
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.RGBA8_UNORM,
                    width,
                    height,
                    1,  // depthOrLayers
                    1   // mipLevels
            );
            textureView = device.createTextureView(texture);

            textureWidth = width;
            textureHeight = height;

            // Point the direct texture wrapper at our new texture/view
            if (directTexture != null) {
                directTexture.setBackingTexture(texture, textureView);
            }
        }

        writeRegion(buffer, width, 0, 0, 0, 0, width, height);
    }

    /**
     * Partial (dirty-rect) paint into the existing texture.
     *
     * @param buffer       the source frame buffer (may be larger than the region being written)
     * @param bufferStride the width, in pixels, of a row in {@code buffer}
     * @param srcX         the x offset, in pixels, of the region within {@code buffer}
     * @param srcY         the y offset, in pixels, of the region within {@code buffer}
     * @param destX        the x offset, in pixels, within the destination texture
     * @param destY        the y offset, in pixels, within the destination texture
     */
    protected void onPaint(ByteBuffer buffer, int bufferStride, int srcX, int srcY, int destX, int destY, int width, int height) {
        if (texture == null) return;
        writeRegion(buffer, bufferStride, srcX, srcY, destX, destY, width, height);
    }

    private void writeRegion(ByteBuffer buffer, int bufferStride, int srcX, int srcY, int destX, int destY, int width, int height) {
        if (width <= 0 || height <= 0) return;

        int needed = width * height * 4;
        if (scratch == null || scratch.capacity() < needed) {
            if (scratch != null) MemoryUtil.memFree(scratch);
            scratch = MemoryUtil.memAlloc(needed);
        }
        scratch.clear();
        scratch.limit(needed);

        // Repack the requested sub-rectangle into a tightly-packed buffer, swapping CEF's
        // BGRA8 byte order to the RGBA8 order our GpuFormat.RGBA8_UNORM texture expects.
        for (int row = 0; row < height; row++) {
            int srcRowStart = ((srcY + row) * bufferStride + srcX) * 4;
            for (int col = 0; col < width; col++) {
                int srcIndex = srcRowStart + col * 4;
                byte b = buffer.get(srcIndex);
                byte g = buffer.get(srcIndex + 1);
                byte r = buffer.get(srcIndex + 2);
                byte a = buffer.get(srcIndex + 3);
                scratch.put(r).put(g).put(b).put(a);
            }
        }
        scratch.flip();

        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(texture, scratch, 0, 0, destX, destY, width, height);
    }
}
