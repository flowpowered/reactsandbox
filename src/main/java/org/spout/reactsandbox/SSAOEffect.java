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

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.Uniform.Vector3ArrayUniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.util.CausticUtil;

public class SSAOEffect {
    private final int kernelSize;
    private final Vector3f[] kernel;
    private final float radius;
    private final float threshold;
    private final Vector2f noiseScale;
    private final Texture noiseTexture;
    private final float power;

    public SSAOEffect(Context context, Vector2i resolution, int kernelSize, int noiseSize, float radius, float threshold, float power) {
        this.kernelSize = kernelSize;
        this.kernel = new Vector3f[kernelSize];
        this.radius = radius;
        this.threshold = threshold;
        this.noiseScale = resolution.toFloat().div(noiseSize);
        this.noiseTexture = context.newTexture();
        this.power = power;
        // Generate the kernel
        final Random random = new Random();
        for (int i = 0; i < kernelSize; i++) {
            float scale = (float) i / kernelSize;
            scale = GenericMath.lerp(threshold, 1, scale * scale);
            // Create a set of random unit vectors inside a hemisphere
            // The vectors are scaled so that the amount falls of as we get further away from the center
            kernel[i] = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat()).normalize().mul(scale);
        }
        // Generate the noise texture
        final int noiseTextureSize = noiseSize * noiseSize;
        final ByteBuffer noiseTextureBuffer = CausticUtil.createByteBuffer(noiseTextureSize * 3);
        for (int i = 0; i < noiseTextureSize; i++) {
            // Random unit vectors around the z axis
            Vector3f noise = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, 0).normalize();
            // Encode to unsigned byte, and place in buffer
            noise = noise.mul(128).add(128, 128, 128);
            noiseTextureBuffer.put((byte) (noise.getFloorX() & 0xff));
            noiseTextureBuffer.put((byte) (noise.getFloorY() & 0xff));
            noiseTextureBuffer.put((byte) (noise.getFloorZ() & 0xff));
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
        destination.add(new Vector3ArrayUniform("kernel", kernel));
        destination.add(new FloatUniform("radius", radius));
        destination.add(new FloatUniform("threshold", threshold));
        destination.add(new Vector2Uniform("noiseScale", noiseScale));
        destination.add(new FloatUniform("power", power));
    }
}
