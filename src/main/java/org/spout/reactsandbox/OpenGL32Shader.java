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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.OpenGLException;

public class OpenGL32Shader {
	private final int id;

	private OpenGL32Shader(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}

	public void destroy() {
		GL20.glDeleteShader(id);
	}

	public static OpenGL32Shader create(InputStream shaderResource, int type) {
		final StringBuilder shaderSource = new StringBuilder();
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(shaderResource));
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("\n");
			}
			reader.close();
			shaderResource.close();
		} catch (IOException e) {
			System.out.println("IO exception: " + e.getMessage());
		}
		final int shaderID = GL20.glCreateShader(type);
		GL20.glShaderSource(shaderID, shaderSource);
		GL20.glCompileShader(shaderID);
		if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			throw new OpenGLException("OPEN GL ERROR: Could not compile shader\n" + GL20.glGetShaderInfoLog(shaderID, 1000));
		}
		OpenGL32Renderer.checkForOpenGLError("loadShader");
		return new OpenGL32Shader(shaderID);
	}
}
