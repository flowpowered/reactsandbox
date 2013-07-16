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

import java.awt.Color;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import org.spout.physics.math.Matrix4x4;
import org.spout.physics.math.Vector3;

/**
 * Represents a program for OpenGL 3.2. A program is a composed of a vertex shader and a fragment
 * shader. After being constructed, the program needs to be created in the OpenGL context with
 * {@link #create(java.io.InputStream, java.io.InputStream)}.
 */
public class OpenGL32Program {
	// State
	private boolean created = false;
	// ID
	private int id;
	// Shaders
	private final OpenGL32Shader vert = new OpenGL32Shader();
	private final OpenGL32Shader frag = new OpenGL32Shader();
	// Map of the uniform name and IDs
	private final Map<String, Integer> uniforms = new HashMap<String, Integer>();

	/**
	 * Creates a new program in the OpenGL context from the input streams for the vertex and fragment
	 * shaders.
	 *
	 * @param vertShader The vertex shader input stream
	 * @param fragShader The fragment shader input stream
	 */
	public void create(InputStream vertShader, InputStream fragShader) {
		if (created) {
			throw new IllegalStateException("Program has already been created.");
		}
		vert.create(vertShader, GL20.GL_VERTEX_SHADER);
		frag.create(fragShader, GL20.GL_FRAGMENT_SHADER);
		id = GL20.glCreateProgram();
		GL20.glAttachShader(id, vert.getID());
		GL20.glAttachShader(id, frag.getID());
		GL20.glLinkProgram(id);
		GL20.glValidateProgram(id);
		final int uniformCount = GL20.glGetProgrami(id, GL20.GL_ACTIVE_UNIFORMS);
		for (int i = 0; i < uniformCount; i++) {
			final ByteBuffer nameBuffer = BufferUtils.createByteBuffer(256);
			GL20.glGetActiveUniform(id, i,
					BufferUtils.createIntBuffer(1),
					BufferUtils.createIntBuffer(1),
					BufferUtils.createIntBuffer(1),
					nameBuffer);
			nameBuffer.rewind();
			final byte[] nameBytes = new byte[256];
			nameBuffer.get(nameBytes);
			final String name = new String(nameBytes).trim();
			uniforms.put(name, GL20.glGetUniformLocation(id, name));
		}
		created = true;
	}

	/**
	 * Destroys this program by deleting the OpenGL shaders program.
	 */
	public void destroy() {
		if (!created) {
			throw new IllegalStateException("Program has not been created yet.");
		}
		GL20.glDetachShader(id, vert.getID());
		GL20.glDetachShader(id, frag.getID());
		vert.destroy();
		frag.destroy();
		GL20.glDeleteProgram(id);
		id = 0;
		uniforms.clear();
		created = false;
	}

	/**
	 * Gets the ID for this program as assigned by OpenGL.
	 *
	 * @return The ID
	 */
	public int getID() {
		return id;
	}

	/**
	 * Sets a uniform boolean in the shader to the desired value.
	 *
	 * @param name The name of the uniform to set
	 * @param b The boolean value
	 */
	public void setUniform(String name, boolean b) {
		if (!uniforms.containsKey(name)) {
			throw new IllegalArgumentException("The uniform name could not be found in the program");
		}
		GL20.glUniform1i(uniforms.get(name), b ? 1 : 0);
	}

	/**
	 * Sets a uniform float in the shader to the desired value.
	 *
	 * @param name The name of the uniform to set
	 * @param f The float value
	 */
	public void setUniform(String name, float f) {
		if (!uniforms.containsKey(name)) {
			throw new IllegalArgumentException("The uniform name could not be found in the program");
		}
		GL20.glUniform1f(uniforms.get(name), f);
	}

	/**
	 * Sets a uniform {@link org.spout.physics.math.Vector3} in the shader to the desired value.
	 *
	 * @param name The name of the uniform to set
	 * @param v The vector value
	 */
	public void setUniform(String name, Vector3 v) {
		if (!uniforms.containsKey(name)) {
			throw new IllegalArgumentException("The uniform name could not be found in the program");
		}
		GL20.glUniform3f(uniforms.get(name), v.getX(), v.getY(), v.getZ());
	}

	/**
	 * Sets a uniform {@link org.spout.physics.math.Matrix4x4} in the shader to the desired value.
	 *
	 * @param name The name of the uniform to set
	 * @param m The matrix value
	 */
	public void setUniform(String name, Matrix4x4 m) {
		if (!uniforms.containsKey(name)) {
			throw new IllegalArgumentException("The uniform name could not be found in the program");
		}
		final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		buffer.put(SandboxUtil.asArray(m));
		buffer.flip();
		GL20.glUniformMatrix4(uniforms.get(name), false, buffer);
	}

	/**
	 * Sets a uniform {@link java.awt.Color} in the shader to the desired value.
	 *
	 * @param name The name of the uniform to set
	 * @param c The color value
	 */
	public void setUniform(String name, Color c) {
		if (!uniforms.containsKey(name)) {
			throw new IllegalArgumentException("The uniform name could not be found in the program");
		}
		GL20.glUniform4f(uniforms.get(name),
				c.getRed() / 255f, c.getGreen() / 255f,
				c.getBlue() / 255f, c.getAlpha() / 255f);
	}

	/**
	 * Returns an immutable set containing all of the uniform names for this program.
	 *
	 * @return A set of all the uniform names
	 */
	public Set<String> getUniformNames() {
		return Collections.unmodifiableSet(uniforms.keySet());
	}
}
