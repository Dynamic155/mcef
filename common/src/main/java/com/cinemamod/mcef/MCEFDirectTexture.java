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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

/**
 * A thin {@link AbstractTexture} that points at the {@link GpuTexture}/{@link GpuTextureView}
 * owned and updated by {@link MCEFRenderer}, so the browser's texture can be referenced by a
 * {@link net.minecraft.resources.ResourceLocation} anywhere Minecraft's rendering APIs expect one
 * (e.g. GuiGraphics#blit). MCEFDirectTexture does not own the backing texture's lifecycle --
 * MCEFRenderer creates, resizes and closes it.
 */
public class MCEFDirectTexture extends AbstractTexture {
    public MCEFDirectTexture() {
    }

    /**
     * Points this texture at MCEFRenderer's current backing texture/view.
     */
    void setBackingTexture(GpuTexture texture, GpuTextureView textureView) {
        this.texture = texture;
        this.textureView = textureView;
        // Crisp text/UI over smoothing, matching the browser's own pixels 1:1.
        this.sampler = RenderSystem.getSamplerCache().getSampler(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.NEAREST, FilterMode.NEAREST, false);
    }

    @Override
    public void close() {
        // Don't close the backing texture/view here -- MCEFRenderer owns and closes them.
        this.texture = null;
        this.textureView = null;
    }
}
