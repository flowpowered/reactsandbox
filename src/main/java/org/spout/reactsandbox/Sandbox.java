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
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.yaml.snakeyaml.Yaml;

import org.spout.physics.math.Quaternion;
import org.spout.physics.math.Vector3;

public class Sandbox {
	// Constants
	private static final String WINDOW_TITLE = "React Sandbox";
	// Settings
	private static float mouseSensitivity = 0.08f;
	private static float cameraSpeed = 0.2f;
	private static int windowWidth = 1200;
	private static int windowHeight = 800;
	private static float fieldOfView = 75;
	private static Color defaultModelColor;
	// Model data
	// Input
	private static boolean mouseGrabbed = true;
	private static float cameraPitch = 0;
	private static float cameraYaw = 0;

	public static void main(String[] args) {
		try {
			deploy();
			loadConfiguration();
			System.out.println("Starting up");
			OpenGL32Renderer.create(WINDOW_TITLE, windowWidth, windowHeight, fieldOfView);
			final OpenGL32Model model = new OpenGL32Model();
			MeshGenerator.generateSphericalMesh(model, 3);
			model.color(defaultModelColor);
			model.create();
			OpenGL32Renderer.addModel(model);
			Mouse.setGrabbed(true);
			while (!Display.isCloseRequested()) {
				final long start = System.nanoTime();
				processInput();
				OpenGL32Renderer.render();
				Thread.sleep(Math.max(20 - Math.round((System.nanoTime() - start) / 1000000d), 0));
			}
			System.out.println("Shutting down");
			Mouse.setGrabbed(false);
			OpenGL32Renderer.destroy();
		} catch (Exception ex) {
			ex.printStackTrace();
			final String name = ex.getClass().getSimpleName();
			final String message = ex.getMessage();
			Sys.alert("Error: " + name, message == null || message.trim().equals("") ? name : message);
			System.exit(-1);
		}
	}

	private static void processInput() {
		final boolean mouseGrabbedBefore = mouseGrabbed;
		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
					mouseGrabbed ^= true;
				}
			}
		}
		if (mouseGrabbed != mouseGrabbedBefore) {
			Mouse.setGrabbed(mouseGrabbed);
		}
		if (mouseGrabbed) {
			cameraYaw -= Mouse.getDY() * mouseSensitivity;
			cameraYaw %= 360;
			cameraPitch += Mouse.getDX() * mouseSensitivity;
			cameraPitch %= 360;
			final Quaternion yaw = MathHelper.angleAxisToQuaternion(cameraYaw, 1, 0, 0);
			final Quaternion pitch = MathHelper.angleAxisToQuaternion(cameraPitch, 0, 1, 0);
			OpenGL32Renderer.cameraRotation(Quaternion.multiply(yaw, pitch));
		}
		final Vector3 right = OpenGL32Renderer.cameraRight();
		final Vector3 up = OpenGL32Renderer.cameraUp();
		final Vector3 forward = OpenGL32Renderer.cameraForward();
		final Vector3 position = OpenGL32Renderer.cameraPosition();
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			position.add(Vector3.multiply(forward, cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			position.add(Vector3.multiply(forward, -cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			position.add(Vector3.multiply(right, cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			position.add(Vector3.multiply(right, -cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			position.add(Vector3.multiply(up, cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			position.add(Vector3.multiply(up, -cameraSpeed));
		}
		OpenGL32Renderer.lightPosition(Vector3.negate(position));
	}

	private static void deploy() throws Exception {
		final File configFile = new File("config.yml");
		if (!configFile.exists()) {
			FileUtils.copyInputStreamToFile(Sandbox.class.getResourceAsStream("/config.yml"), configFile);
		}
		final String osPath;
		final String[] nativeLibs;
		if (SystemUtils.IS_OS_WINDOWS) {
			nativeLibs = new String[]{
					"jinput-dx8_64.dll", "jinput-dx8.dll", "jinput-raw_64.dll", "jinput-raw.dll",
					"jinput-wintab.dll", "lwjgl.dll", "lwjgl64.dll", "OpenAL32.dll", "OpenAL64.dll"
			};
			osPath = "windows/";
		} else if (SystemUtils.IS_OS_MAC) {
			nativeLibs = new String[]{
					"libjinput-osx.jnilib", "liblwjgl.jnilib", "openal.dylib"
			};
			osPath = "mac/";
		} else if (SystemUtils.IS_OS_LINUX) {
			nativeLibs = new String[]{
					"liblwjgl.so", "liblwjgl64.so", "libopenal.so", "libopenal64.so", "libjinput-linux.so",
					"libjinput-linux64.so"
			};
			osPath = "linux/";
		} else {
			throw new IllegalStateException("Could not get lwjgl natives for OS \"" + SystemUtils.OS_NAME + "\".");
		}
		final File nativesDir = new File("natives" + File.separator + osPath);
		nativesDir.mkdirs();
		for (String nativeLib : nativeLibs) {
			final File nativeFile = new File(nativesDir, nativeLib);
			if (!nativeFile.exists()) {
				FileUtils.copyInputStreamToFile(Sandbox.class.getResourceAsStream("/" + nativeLib), nativeFile);
			}
		}
		final String nativesPath = nativesDir.getAbsolutePath();
		System.setProperty("org.lwjgl.librarypath", nativesPath);
		System.setProperty("net.java.games.input.librarypath", nativesPath);
	}

	@SuppressWarnings("unchecked")
	private static void loadConfiguration() throws Exception {
		try {
			final Map<String, Object> config =
					(Map<String, Object>) new Yaml().load(new FileInputStream("config.yml"));
			final Map<String, Object> inputConfig = (Map<String, Object>) config.get("Input");
			final Map<String, Object> appearanceConfig = (Map<String, Object>) config.get("Appearance");
			mouseSensitivity = ((Number) inputConfig.get("MouseSensitivity")).floatValue();
			cameraSpeed = ((Number) inputConfig.get("CameraSpeed")).floatValue();
			final String[] windowSize = ((String) appearanceConfig.get("WindowSize")).split(",");
			windowWidth = Integer.parseInt(windowSize[0].trim());
			windowHeight = Integer.parseInt(windowSize[1].trim());
			fieldOfView = ((Number) appearanceConfig.get("FieldOfView")).floatValue();
			OpenGL32Renderer.backgroundColor(parseColor(((String) appearanceConfig.get("BackgroundColor")), 0));
			defaultModelColor = (parseColor(((String) appearanceConfig.get("ModelColor")), 1));
			OpenGL32Renderer.diffuseIntensity(((Number) appearanceConfig.get("DiffuseIntensity")).floatValue());
			OpenGL32Renderer.specularIntensity(((Number) appearanceConfig.get("SpecularIntensity")).floatValue());
			OpenGL32Renderer.ambientIntensity(((Number) appearanceConfig.get("AmbientIntensity")).floatValue());
			OpenGL32Renderer.lightAttenuation(((Number) appearanceConfig.get("LightAttenuation")).floatValue());
		} catch (Exception ex) {
			throw new IllegalStateException("Malformed config.yml: \"" + ex.getMessage() + "\".");
		}
	}

	private static Color parseColor(String s, float alpha) {
		final String[] ss = s.split(",");
		return new Color(
				Float.parseFloat(ss[0].trim()),
				Float.parseFloat(ss[1].trim()),
				Float.parseFloat(ss[2].trim()),
				alpha);
	}
}
