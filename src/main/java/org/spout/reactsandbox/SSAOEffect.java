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

import org.spout.math.GenericMath;
import org.spout.math.vector.Vector2;
import org.spout.math.vector.Vector3;
import org.spout.renderer.data.Uniform.FloatUniform;
import org.spout.renderer.data.Uniform.IntUniform;
import org.spout.renderer.data.Uniform.Vector2Uniform;
import org.spout.renderer.data.Uniform.Vector3ArrayUniform;
import org.spout.renderer.data.UniformHolder;
import org.spout.renderer.data.VertexAttribute.DataType;
import org.spout.renderer.gl.GLFactory;
import org.spout.renderer.gl.Texture;
import org.spout.renderer.gl.Texture.Format;
import org.spout.renderer.gl.Texture.InternalFormat;
import org.spout.renderer.util.CausticUtil;

/**
 *
 */
public class SSAOEffect {
	private final int kernelSize;
	private final Vector3[] kernel;
	private final float radius;
	private final Vector2 noiseScale;
	private final Texture noiseTexture;
	private final float power;
	private final int noiseSize;
	private final Vector2 texelSize;

	public SSAOEffect(GLFactory glFactory, Vector2 resolution, int kernelSize, int noiseSize, float radius, float power) {
		this.kernelSize = kernelSize;
		this.kernel = new Vector3[kernelSize];
		this.radius = radius;
		this.noiseScale = resolution.div(noiseSize);
		this.noiseTexture = glFactory.createTexture();
		this.power = power;
		this.noiseSize = noiseSize;
		this.texelSize = new Vector2(1, 1).div(resolution);
		// Generate the kernel
		final Random random = new Random();
		for (int i = 0; i < kernelSize; i++) {
			float scale = (float) i / kernelSize;
			scale = GenericMath.lerp(0.1f, 1, scale * scale);
			// Create a set of random unit vectors inside a hemisphere
			// The vectors are scaled so that the amount falls of as we get further away from the center
			kernel[i] = new Vector3(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat()).normalize().mul(scale);
		}
		// Generate the noise texture
		final int noiseTextureSize = noiseSize * noiseSize;
		// 3 floats components = 12 bytes per pixel
		final ByteBuffer noiseTextureBuffer = CausticUtil.createByteBuffer(noiseTextureSize * 12);
		for (int i = 0; i < noiseTextureSize; i++) {
			// Random unit vectors around the z axis
			final Vector3 noise = new Vector3(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, 0).normalize();
			noiseTextureBuffer.putFloat(noise.getX());
			noiseTextureBuffer.putFloat(noise.getY());
			noiseTextureBuffer.putFloat(noise.getZ());
		}
		noiseTexture.setFormat(Format.RGB);
		noiseTexture.setInternalFormat(InternalFormat.RGB32F);
		noiseTexture.setComponentType(DataType.FLOAT);
		noiseTextureBuffer.flip();
		noiseTexture.setImageData(noiseTextureBuffer, noiseSize, noiseSize);
		noiseTexture.create();
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
		destination.add(new Vector2Uniform("noiseScale", noiseScale));
		destination.add(new FloatUniform("power", power));
		destination.add(new IntUniform("noiseSize", noiseSize));
		destination.add(new Vector2Uniform("texelSize", texelSize));
	}
}
