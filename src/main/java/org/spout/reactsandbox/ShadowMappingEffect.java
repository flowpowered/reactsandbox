/*
 * This file is part of ReactSandbox.
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
 * ReactSandbox is licensed under the Spout License Version 1.
 *
 * ReactSandbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * ReactSandbox is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.reactsandbox;

import java.nio.ByteBuffer;
import java.util.Random;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;

import com.flowpowered.caustic.api.data.Uniform.FloatUniform;
import com.flowpowered.caustic.api.data.Uniform.IntUniform;
import com.flowpowered.caustic.api.data.Uniform.Vector2ArrayUniform;
import com.flowpowered.caustic.api.data.Uniform.Vector2Uniform;
import com.flowpowered.caustic.api.data.UniformHolder;
import com.flowpowered.caustic.api.gl.Context;
import com.flowpowered.caustic.api.gl.Texture;
import com.flowpowered.caustic.api.gl.Texture.FilterMode;
import com.flowpowered.caustic.api.gl.Texture.Format;
import com.flowpowered.caustic.api.gl.Texture.InternalFormat;
import com.flowpowered.caustic.api.util.CausticUtil;

public class ShadowMappingEffect {
    private final int kernelSize;
    private final Vector2f[] kernel;
    private final Vector2f noiseScale;
    private final Texture noiseTexture;
    private final float bias;
    private final float radius;

    public ShadowMappingEffect(Context context, Vector2i resolution, int kernelSize, int noiseSize, float bias, float radius) {
        this.kernelSize = kernelSize;
        this.kernel = new Vector2f[kernelSize];
        this.noiseScale = resolution.toFloat().div(noiseSize);
        this.noiseTexture = context.newTexture();
        this.bias = bias;
        this.radius = radius;
        // Generate the kernel
        final Random random = new Random();
        for (int i = 0; i < kernelSize; i++) {
            // Create a set of random unit vectors
            kernel[i] = new Vector2f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
        }
        // Generate the noise texture
        final int noiseTextureSize = noiseSize * noiseSize;
        final ByteBuffer noiseTextureBuffer = CausticUtil.createByteBuffer(noiseTextureSize * 3);
        for (int i = 0; i < noiseTextureSize; i++) {
            // Random unit vectors around the z axis
            Vector2f noise = new Vector2f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
            // Encode to unsigned byte, and place in buffer
            noise = noise.mul(128).add(128, 128);
            noiseTextureBuffer.put((byte) (noise.getFloorX() & 0xff));
            noiseTextureBuffer.put((byte) (noise.getFloorY() & 0xff));
            noiseTextureBuffer.put((byte) 0);
        }
        noiseTextureBuffer.flip();
        noiseTexture.create();
        noiseTexture.setFormat(Format.RGB, InternalFormat.RGB8);
        noiseTexture.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
        noiseTexture.setImageData(noiseTextureBuffer, noiseSize, noiseSize);
    }

    public void dispose() {
        noiseTexture.destroy();
    }

    public Texture getNoiseTexture() {
        return noiseTexture;
    }

    public void addUniforms(UniformHolder destination) {
        destination.add(new IntUniform("kernelSize", kernelSize));
        destination.add(new Vector2ArrayUniform("kernel", kernel));
        destination.add(new Vector2Uniform("noiseScale", noiseScale));
        destination.add(new FloatUniform("bias", bias));
        destination.add(new FloatUniform("radius", radius));
    }
}
