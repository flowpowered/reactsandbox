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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import org.spout.physics.math.Matrix4x4;
import org.spout.physics.math.Vector3;

public class OpenGL32Program {
	private final int id;
	private final OpenGL32Shader vert;
	private final OpenGL32Shader frag;
	private final Map<String, Integer> uniforms = new HashMap<String, Integer>();

	private OpenGL32Program(int id, OpenGL32Shader vert, OpenGL32Shader frag) {
		this.id = id;
		this.vert = vert;
		this.frag = frag;
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
	}

	public static OpenGL32Program create(InputStream vertShader, InputStream fragShader) {
		final OpenGL32Shader vert = OpenGL32Shader.create(vertShader, GL20.GL_VERTEX_SHADER);
		final OpenGL32Shader frag = OpenGL32Shader.create(fragShader, GL20.GL_FRAGMENT_SHADER);
		final int id = GL20.glCreateProgram();
		GL20.glAttachShader(id, vert.getID());
		GL20.glAttachShader(id, frag.getID());
		GL20.glLinkProgram(id);
		GL20.glValidateProgram(id);
		return new OpenGL32Program(id, vert, frag);
	}

	public int getID() {
		return id;
	}

	public void setUniform(String name, float f) {
		GL20.glUniform1f(uniforms.get(name), f);
	}

	public void setUniform(String name, Vector3 v) {
		GL20.glUniform3f(uniforms.get(name), v.getX(), v.getY(), v.getZ());
	}

	public void setUniform(String name, Matrix4x4 m) {
		final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		buffer.put(SandboxUtil.asArray(m));
		buffer.flip();
		GL20.glUniformMatrix4(uniforms.get(name), false, buffer);
	}

	public void setUniform(String name, Color c) {
		GL20.glUniform4f(uniforms.get(name),
				c.getRed() / 255f, c.getGreen() / 255f,
				c.getBlue() / 255f, c.getAlpha() / 255f);
	}

	public Set<String> getUniformNames() {
		return uniforms.keySet();
	}

	public void destroy() {
		GL20.glDetachShader(id, vert.getID());
		GL20.glDetachShader(id, frag.getID());
		vert.destroy();
		frag.destroy();
		GL20.glDeleteProgram(id);
	}
}
