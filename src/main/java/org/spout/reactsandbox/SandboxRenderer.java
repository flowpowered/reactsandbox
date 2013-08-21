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

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.spout.math.imaginary.Quaternion;
import org.spout.math.vector.Vector2;
import org.spout.math.vector.Vector3;
import org.spout.renderer.Camera;
import org.spout.renderer.GLVersioned.GLVersion;
import org.spout.renderer.Material;
import org.spout.renderer.Model;
import org.spout.renderer.data.Color;
import org.spout.renderer.data.RenderList;
import org.spout.renderer.data.Uniform.ColorUniform;
import org.spout.renderer.data.Uniform.FloatUniform;
import org.spout.renderer.data.Uniform.Vector2Uniform;
import org.spout.renderer.data.Uniform.Vector3Uniform;
import org.spout.renderer.data.UniformHolder;
import org.spout.renderer.gl.Capability;
import org.spout.renderer.gl.FrameBuffer;
import org.spout.renderer.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.gl.GLFactory;
import org.spout.renderer.gl.Program;
import org.spout.renderer.gl.Renderer;
import org.spout.renderer.gl.Shader;
import org.spout.renderer.gl.Shader.ShaderType;
import org.spout.renderer.gl.Texture;
import org.spout.renderer.gl.Texture.FilterMode;
import org.spout.renderer.gl.Texture.Format;
import org.spout.renderer.gl.Texture.InternalFormat;
import org.spout.renderer.gl.Texture.WrapMode;
import org.spout.renderer.gl.VertexArray;
import org.spout.renderer.gl.VertexArray.DrawingMode;
import org.spout.renderer.lwjgl.gl20.GL20GLFactory;
import org.spout.renderer.lwjgl.gl30.GL30GLFactory;
import org.spout.renderer.util.CausticUtil;
import org.spout.renderer.util.InstancedModel;
import org.spout.renderer.util.InstancedStringModel;
import org.spout.renderer.util.ObjFileLoader;
import org.spout.renderer.util.StringModel;

/**
 *
 */
public class SandboxRenderer {
	// CONSTANTS
	private static final String WINDOW_TITLE = "Sandbox";
	private static final Vector2 WINDOW_SIZE = new Vector2(1200, 800);
	private static final float ASPECT_RATIO = WINDOW_SIZE.getX() / WINDOW_SIZE.getY();
	private static final float FIELD_OF_VIEW = 60;
	private static final float TAN_HALF_FOV = (float) Math.tan(Math.toRadians(FIELD_OF_VIEW) / 2);
	private static final float NEAR_PLANE = 0.1f;
	private static final float FAR_PLANE = 1000;
	// SETTINGS
	private static Color backgroundColor = Color.DARK_GRAY;
	private static boolean cullBackFaces = true;
	// LIGHTING UNIFORMS
	private static final Vector3Uniform lightPositionUniform = new Vector3Uniform("lightPosition", Vector3.ZERO);
	private static final FloatUniform diffuseIntensityUniform = new FloatUniform("diffuseIntensity", 0.8f);
	private static final FloatUniform specularIntensityUniform = new FloatUniform("specularIntensity", 0.2f);
	private static final FloatUniform ambientIntensityUniform = new FloatUniform("ambientIntensity", 0.3f);
	private static final FloatUniform lightAttenuationUniform = new FloatUniform("lightAttenuation", 0.03f);
	// CAMERAS
	private static final Camera modelCamera = Camera.createPerspective(FIELD_OF_VIEW, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), NEAR_PLANE, FAR_PLANE);
	private static final Camera guiCamera = Camera.createOrthographic(1, 0, 1 / ASPECT_RATIO, 0, NEAR_PLANE, FAR_PLANE);
	// OPENGL VERSION AND FACTORY
	private static GLVersion glVersion;
	private static GLFactory glFactory;
	// RENDERER
	private static Renderer renderer;
	// RENDER LISTS
	private static final RenderList modelRenderList = new RenderList("models", modelCamera, 0);
	private static final RenderList ssaoRenderList = new RenderList("ssao", modelCamera, 1);
	private static final RenderList ssaoBlurRenderList = new RenderList("ssao blur", modelCamera, 2);
	private static final RenderList lightingRenderList = new RenderList("lighting", modelCamera, 3);
	private static final RenderList antiAliasingRenderList = new RenderList("antiAliasing", modelCamera, 4);
	private static final RenderList guiRenderList = new RenderList("gui", guiCamera, 5);
	// SHADERS
	private static Shader solidVert;
	private static Shader solidFrag;
	private static Shader texturedVert;
	private static Shader texturedFrag;
	private static Shader ssaoVert;
	private static Shader ssaoFrag;
	private static Shader ssaoBlurVert;
	private static Shader ssaoBlurFrag;
	private static Shader lightingVert;
	private static Shader lightingFrag;
	private static Shader antiAliasingVert;
	private static Shader antiAliasingFrag;
	private static Shader screenVert;
	private static Shader screenFrag;
	// PROGRAMS
	private static Program solidProgram;
	private static Program texturedProgram;
	private static Program ssaoProgram;
	private static Program ssaoBlurProgram;
	private static Program lightingProgram;
	private static Program antiAliasingProgram;
	private static Program screenProgram;
	// TEXTURES
	private static Texture creeperSkinTexture;
	private static Texture woodDiffuseTexture;
	private static Texture colorsTexture;
	private static Texture normalsTexture;
	private static Texture depthsTexture;
	private static Texture ssaoTexture;
	private static Texture ssaoBlurTexture;
	private static Texture auxColorsTexture;
	// MATERIALS
	private static Material solidMaterial;
	private static Material creeperMaterial;
	private static Material woodMaterial;
	private static Material ssaoMaterial;
	private static Material ssaoBlurMaterial;
	private static Material lightingMaterial;
	private static Material antiAliasingMaterial;
	private static Material screenMaterial;
	// FRAME BUFFERS
	private static FrameBuffer modelFrameBuffer;
	private static FrameBuffer ssaoFrameBuffer;
	private static FrameBuffer ssaoBlurFrameBuffer;
	private static FrameBuffer lightingFrameBuffer;
	private static FrameBuffer antiAliasingFrameBuffer;
	// VERTEX ARRAYS
	private static VertexArray unitCubeWireVertexArray;
	private static VertexArray diamondModelVertexArray;
	// SSAO
	private static SSAOEffect ssaoEffect;
	// MODEL PROPERTIES
	private static Color aabbModelColor;
	private static Color diamondModelColor;
	private static Color cylinderModelColor;
	private static Color sphereModelColor;
	// FPS MONITOR
	private static final FPSMonitor fpsMonitor = new FPSMonitor();
	private static InstancedStringModel fpsMonitorModel;

	public static void init() {
		initRenderer();
		initRenderLists();
		initShaders();
		initPrograms();
		initTextures();
		initMaterials();
		initFrameBuffers();
		initVertexArrays();
	}

	private static void initRenderer() {
		// RENDERER
		renderer = glFactory.createRenderer();
		renderer.setWindowTitle(WINDOW_TITLE);
		renderer.setWindowSize(WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		renderer.create();
		renderer.setClearColor(new Color(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), 0));
		// SSAO
		ssaoEffect = new SSAOEffect(glFactory, WINDOW_SIZE, 8, 4, 0.5f, 2);
	}

	private static void initRenderLists() {
		// MODEL
		modelRenderList.addCapability(Capability.DEPTH_TEST);
		if (cullBackFaces) {
			modelRenderList.addCapability(Capability.CULL_FACE);
		}
		if (glVersion == GLVersion.GL30) {
			modelRenderList.addCapability(Capability.DEPTH_CLAMP);
		}
		renderer.addRenderList(modelRenderList);
		// SSAO
		if (cullBackFaces) {
			ssaoRenderList.addCapability(Capability.CULL_FACE);
		}
		renderer.addRenderList(ssaoRenderList);
		// SSAO BLUR
		if (cullBackFaces) {
			ssaoBlurRenderList.addCapability(Capability.CULL_FACE);
		}
		renderer.addRenderList(ssaoBlurRenderList);
		// LIGHTING
		if (cullBackFaces) {
			lightingRenderList.addCapability(Capability.CULL_FACE);
		}
		renderer.addRenderList(lightingRenderList);
		// ANTI ALIASING
		if (cullBackFaces) {
			antiAliasingRenderList.addCapability(Capability.CULL_FACE);
		}
		renderer.addRenderList(antiAliasingRenderList);
		// GUI
		guiRenderList.addCapabilities(Capability.DEPTH_TEST, Capability.BLEND);
		if (cullBackFaces) {
			guiRenderList.addCapability(Capability.CULL_FACE);
		}
		renderer.addRenderList(guiRenderList);
	}

	private static void initShaders() {
		final String shaderPath = "/shaders/" + glVersion.toString().toLowerCase() + "/";
		// SOLID VERT
		solidVert = glFactory.createShader();
		solidVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "solid.vert"));
		solidVert.setType(ShaderType.VERTEX);
		solidVert.create();
		// SOLID FRAG
		solidFrag = glFactory.createShader();
		solidFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "solid.frag"));
		solidFrag.setType(ShaderType.FRAGMENT);
		solidFrag.create();
		// TEXTURED VERT
		texturedVert = glFactory.createShader();
		texturedVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "textured.vert"));
		texturedVert.setType(ShaderType.VERTEX);
		texturedVert.create();
		// TEXTURED
		texturedFrag = glFactory.createShader();
		texturedFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "textured.frag"));
		texturedFrag.setType(ShaderType.FRAGMENT);
		texturedFrag.create();
		// SSAO VERT
		ssaoVert = glFactory.createShader();
		ssaoVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "ssao.vert"));
		ssaoVert.setType(ShaderType.VERTEX);
		ssaoVert.create();
		// SSAO FRAG
		ssaoFrag = glFactory.createShader();
		ssaoFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "ssao.frag"));
		ssaoFrag.setType(ShaderType.FRAGMENT);
		ssaoFrag.create();
		// SSAO BLUR VERT
		ssaoBlurVert = glFactory.createShader();
		ssaoBlurVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "ssaoBlur.vert"));
		ssaoBlurVert.setType(ShaderType.VERTEX);
		ssaoBlurVert.create();
		// SSAO BLUR FRAG
		ssaoBlurFrag = glFactory.createShader();
		ssaoBlurFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "ssaoBlur.frag"));
		ssaoBlurFrag.setType(ShaderType.FRAGMENT);
		ssaoBlurFrag.create();
		// LIGHTING VERT
		lightingVert = glFactory.createShader();
		lightingVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "lighting.vert"));
		lightingVert.setType(ShaderType.VERTEX);
		lightingVert.create();
		// LIGHTING FRAG
		lightingFrag = glFactory.createShader();
		lightingFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "lighting.frag"));
		lightingFrag.setType(ShaderType.FRAGMENT);
		lightingFrag.create();
		// ANTI ALIASING VERT
		antiAliasingVert = glFactory.createShader();
		antiAliasingVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "edaa.vert"));
		antiAliasingVert.setType(ShaderType.VERTEX);
		antiAliasingVert.create();
		// ANTI ALIASING FRAG
		antiAliasingFrag = glFactory.createShader();
		antiAliasingFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "edaa.frag"));
		antiAliasingFrag.setType(ShaderType.FRAGMENT);
		antiAliasingFrag.create();
		// SCREEN VERT
		screenVert = glFactory.createShader();
		screenVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "screen.vert"));
		screenVert.setType(ShaderType.VERTEX);
		screenVert.create();
		// SCREEN FRAG
		screenFrag = glFactory.createShader();
		screenFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "screen.frag"));
		screenFrag.setType(ShaderType.FRAGMENT);
		screenFrag.create();
	}

	private static void initPrograms() {
		// SOLID
		solidProgram = glFactory.createProgram();
		solidProgram.addShader(solidVert);
		solidProgram.addShader(solidFrag);
		if (glVersion == GLVersion.GL20) {
			solidProgram.addAttributeLayout("position", 0);
			solidProgram.addAttributeLayout("normal", 1);
		}
		solidProgram.create();
		// TEXTURED
		texturedProgram = glFactory.createProgram();
		texturedProgram.addShader(texturedVert);
		texturedProgram.addShader(texturedFrag);
		if (glVersion == GLVersion.GL20) {
			texturedProgram.addAttributeLayout("position", 0);
			texturedProgram.addAttributeLayout("normal", 1);
			texturedProgram.addAttributeLayout("textureCoords", 2);
		}
		texturedProgram.addTextureLayout("diffuse", 0);
		texturedProgram.create();
		// SSAO
		ssaoProgram = glFactory.createProgram();
		ssaoProgram.addShader(ssaoVert);
		ssaoProgram.addShader(ssaoFrag);
		if (glVersion == GLVersion.GL20) {
			ssaoProgram.addAttributeLayout("position", 0);
		}
		ssaoProgram.addTextureLayout("normals", 0);
		ssaoProgram.addTextureLayout("depths", 1);
		ssaoProgram.addTextureLayout("noise", 2);
		ssaoProgram.create();
		// SSAO BLUR
		ssaoBlurProgram = glFactory.createProgram();
		ssaoBlurProgram.addShader(ssaoBlurVert);
		ssaoBlurProgram.addShader(ssaoBlurFrag);
		if (glVersion == GLVersion.GL20) {
			ssaoBlurProgram.addAttributeLayout("position", 0);
		}
		ssaoBlurProgram.addTextureLayout("occlusion", 0);
		ssaoBlurProgram.create();
		// LIGHTING
		lightingProgram = glFactory.createProgram();
		lightingProgram.addShader(lightingVert);
		lightingProgram.addShader(lightingFrag);
		if (glVersion == GLVersion.GL20) {
			lightingProgram.addAttributeLayout("position", 0);
		}
		lightingProgram.addTextureLayout("colors", 0);
		lightingProgram.addTextureLayout("normals", 1);
		lightingProgram.addTextureLayout("depths", 2);
		lightingProgram.addTextureLayout("occlusion", 3);
		lightingProgram.create();
		// ANTI ALIASING
		antiAliasingProgram = glFactory.createProgram();
		antiAliasingProgram.addShader(antiAliasingVert);
		antiAliasingProgram.addShader(antiAliasingFrag);
		if (glVersion == GLVersion.GL20) {
			antiAliasingProgram.addAttributeLayout("position", 0);
		}
		antiAliasingProgram.addTextureLayout("diffuse", 0);
		antiAliasingProgram.addTextureLayout("normals", 1);
		antiAliasingProgram.addTextureLayout("depths", 2);
		antiAliasingProgram.create();
		// SCREEN
		screenProgram = glFactory.createProgram();
		screenProgram.addShader(screenVert);
		screenProgram.addShader(screenFrag);
		if (glVersion == GLVersion.GL20) {
			screenProgram.addAttributeLayout("position", 0);
		}
		screenProgram.addTextureLayout("diffuse", 0);
		screenProgram.create();
	}

	private static void initTextures() {
		ByteBuffer data;
		final Rectangle size = new Rectangle();
		// CREEPER SKIN
		creeperSkinTexture = glFactory.createTexture();
		data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/creeper.png"), Format.RGB, size);
		data.flip();
		creeperSkinTexture.setImageData(data, (int) size.getWidth(), (int) size.getHeight());
		creeperSkinTexture.create();
		// WOOD DIFFUSE
		woodDiffuseTexture = glFactory.createTexture();
		data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/wood_diffuse.png"), Format.RGB, size);
		data.flip();
		woodDiffuseTexture.setImageData(data, (int) size.getWidth(), (int) size.getHeight());
		woodDiffuseTexture.setMagFilter(FilterMode.LINEAR);
		woodDiffuseTexture.setMinFilter(FilterMode.LINEAR_MIPMAP_LINEAR);
		woodDiffuseTexture.setAnisotropicFiltering(16);
		woodDiffuseTexture.create();
		// COLORS
		colorsTexture = glFactory.createTexture();
		colorsTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		colorsTexture.create();
		// NORMALS
		normalsTexture = glFactory.createTexture();
		normalsTexture.setFormat(Format.RGBA);
		normalsTexture.setInternalFormat(InternalFormat.RGBA8);
		normalsTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		normalsTexture.create();
		// DEPTHS
		depthsTexture = glFactory.createTexture();
		depthsTexture.setFormat(Format.DEPTH);
		depthsTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		depthsTexture.setWrapS(WrapMode.CLAMP_TO_EDGE);
		depthsTexture.setWrapT(WrapMode.CLAMP_TO_EDGE);
		depthsTexture.create();
		// SSAO
		ssaoTexture = glFactory.createTexture();
		ssaoTexture.setFormat(Format.RED);
		ssaoTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		ssaoTexture.create();
		// SSAO BLUR
		ssaoBlurTexture = glFactory.createTexture();
		ssaoBlurTexture.setFormat(Format.RED);
		ssaoBlurTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		ssaoBlurTexture.create();
		// AUX COLOR
		auxColorsTexture = glFactory.createTexture();
		auxColorsTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		auxColorsTexture.setMagFilter(FilterMode.LINEAR);
		auxColorsTexture.setMinFilter(FilterMode.LINEAR);
		auxColorsTexture.create();
	}

	private static void initMaterials() {
		UniformHolder uniforms;
		// SOLID
		solidMaterial = new Material(solidProgram);
		// CREEPER
		creeperMaterial = new Material(texturedProgram);
		creeperMaterial.addTexture(0, creeperSkinTexture);
		// WOOD
		woodMaterial = new Material(texturedProgram);
		woodMaterial.addTexture(0, woodDiffuseTexture);
		// SSAO
		ssaoMaterial = new Material(ssaoProgram);
		ssaoMaterial.addTexture(0, normalsTexture);
		ssaoMaterial.addTexture(1, depthsTexture);
		ssaoMaterial.addTexture(2, ssaoEffect.getNoiseTexture());
		uniforms = ssaoMaterial.getUniforms();
		uniforms.add(new FloatUniform("tanHalfFOV", TAN_HALF_FOV));
		uniforms.add(new FloatUniform("aspectRatio", ASPECT_RATIO));
		ssaoEffect.addUniforms(uniforms);
		// SSAO BLUR
		ssaoBlurMaterial = new Material(ssaoBlurProgram);
		ssaoBlurMaterial.addTexture(0, ssaoTexture);
		uniforms = ssaoBlurMaterial.getUniforms();
		ssaoEffect.addUniforms(uniforms);
		// LIGHTING
		lightingMaterial = new Material(lightingProgram);
		lightingMaterial.addTexture(0, colorsTexture);
		lightingMaterial.addTexture(1, normalsTexture);
		lightingMaterial.addTexture(2, depthsTexture);
		lightingMaterial.addTexture(3, ssaoBlurTexture);
		uniforms = lightingMaterial.getUniforms();
		uniforms.add(new FloatUniform("tanHalfFOV", TAN_HALF_FOV));
		uniforms.add(new FloatUniform("aspectRatio", ASPECT_RATIO));
		uniforms.add(lightPositionUniform);
		uniforms.add(diffuseIntensityUniform);
		uniforms.add(specularIntensityUniform);
		uniforms.add(ambientIntensityUniform);
		uniforms.add(lightAttenuationUniform);
		// ANTI ALIASING
		antiAliasingMaterial = new Material(antiAliasingProgram);
		antiAliasingMaterial.addTexture(0, auxColorsTexture);
		antiAliasingMaterial.addTexture(1, normalsTexture);
		antiAliasingMaterial.addTexture(2, depthsTexture);
		uniforms = antiAliasingMaterial.getUniforms();
		uniforms.add(new Vector2Uniform("resolution", WINDOW_SIZE));
		uniforms.add(new FloatUniform("maxSpan", 8));
		uniforms.add(new Vector2Uniform("barriers", new Vector2(0.8f, 0.5f)));
		uniforms.add(new Vector2Uniform("weights", new Vector2(0.25f, 0.6f)));
		uniforms.add(new FloatUniform("kernel", 0.75f));
		// SCREEN
		screenMaterial = new Material(screenProgram);
		screenMaterial.addTexture(0, colorsTexture);
	}

	private static void initFrameBuffers() {
		// MODEL
		modelFrameBuffer = glFactory.createFrameBuffer();
		modelFrameBuffer.attach(AttachmentPoint.COLOR0, colorsTexture);
		modelFrameBuffer.attach(AttachmentPoint.COLOR1, normalsTexture);
		modelFrameBuffer.attach(AttachmentPoint.DEPTH, depthsTexture);
		modelFrameBuffer.create();
		modelRenderList.setFrameBuffer(modelFrameBuffer);
		// SSAO
		ssaoFrameBuffer = glFactory.createFrameBuffer();
		ssaoFrameBuffer.attach(AttachmentPoint.COLOR0, ssaoTexture);
		ssaoFrameBuffer.create();
		ssaoRenderList.setFrameBuffer(ssaoFrameBuffer);
		// SSAO BLUR
		ssaoBlurFrameBuffer = glFactory.createFrameBuffer();
		ssaoBlurFrameBuffer.attach(AttachmentPoint.COLOR0, ssaoBlurTexture);
		ssaoBlurFrameBuffer.create();
		ssaoBlurRenderList.setFrameBuffer(ssaoBlurFrameBuffer);
		// LIGHTING
		lightingFrameBuffer = glFactory.createFrameBuffer();
		lightingFrameBuffer.attach(AttachmentPoint.COLOR0, auxColorsTexture);
		lightingFrameBuffer.create();
		lightingRenderList.setFrameBuffer(lightingFrameBuffer);
		// ANTI ALIASING
		antiAliasingFrameBuffer = glFactory.createFrameBuffer();
		antiAliasingFrameBuffer.attach(AttachmentPoint.COLOR0, colorsTexture);
		antiAliasingFrameBuffer.create();
		antiAliasingRenderList.setFrameBuffer(antiAliasingFrameBuffer);
	}

	private static void initVertexArrays() {
		// UNIT WIRE CUBE
		unitCubeWireVertexArray = glFactory.createVertexArray();
		unitCubeWireVertexArray.setData(MeshGenerator.generateWireCuboid(null, new org.spout.physics.math.Vector3(1, 1, 1)));
		unitCubeWireVertexArray.setDrawingMode(DrawingMode.LINES);
		unitCubeWireVertexArray.create();
		// DIAMOND MODEL
		diamondModelVertexArray = glFactory.createVertexArray();
		diamondModelVertexArray.setData(ObjFileLoader.load(Sandbox.class.getResourceAsStream("/models/diamond.obj")));
		diamondModelVertexArray.create();
	}

	public static void dispose() {
		disposeRenderLists();
		disposeShaders();
		disposePrograms();
		disposeTextures();
		disposeFrameBuffers();
		disposeVertexArrays();
		disposeRenderer();
	}

	private static void disposeRenderer() {
		// SSAO
		ssaoEffect.dispose();
		// RENDERER
		renderer.destroy();
	}

	private static void disposeRenderLists() {
		// MODEL
		modelRenderList.clear();
		modelRenderList.clearCapabilities();
		// SSAO
		ssaoRenderList.clear();
		ssaoRenderList.clearCapabilities();
		// SSAO BLUR
		ssaoBlurRenderList.clear();
		ssaoBlurRenderList.clearCapabilities();
		// LIGHTING
		lightingRenderList.clear();
		lightingRenderList.clearCapabilities();
		// ANTI ALIASING
		antiAliasingRenderList.clear();
		antiAliasingRenderList.clearCapabilities();
		// GUI
		guiRenderList.clear();
		guiRenderList.clearCapabilities();
	}

	private static void disposeShaders() {
		// SOLID
		solidVert.destroy();
		solidFrag.destroy();
		// TEXTURED
		texturedVert.destroy();
		texturedFrag.destroy();
		// SSAO
		ssaoVert.destroy();
		ssaoFrag.destroy();
		// SSAO BLUR
		ssaoBlurVert.destroy();
		ssaoBlurFrag.destroy();
		// LIGHTING
		lightingVert.destroy();
		lightingFrag.destroy();
		// ANTI ALIASING
		antiAliasingVert.destroy();
		antiAliasingFrag.destroy();
		// SCREEN
		screenVert.destroy();
		screenFrag.destroy();
	}

	private static void disposePrograms() {
		// SOLID
		solidProgram.destroy();
		// TEXTURED
		texturedProgram.destroy();
		// SSAO
		ssaoProgram.destroy();
		// SSAO BLUR
		ssaoBlurProgram.destroy();
		// LIGHTING
		lightingProgram.destroy();
		// ANTI ALIASING
		antiAliasingProgram.destroy();
		// SCREEN
		screenProgram.destroy();
	}

	private static void disposeTextures() {
		// CREEPER SKIN
		creeperSkinTexture.destroy();
		// WOOD DIFFUSE
		woodDiffuseTexture.destroy();
		// COLOR
		colorsTexture.destroy();
		// NORMALS
		normalsTexture.destroy();
		// DEPTH
		depthsTexture.destroy();
		// SSAO
		ssaoTexture.destroy();
		// SSAO BLUR
		ssaoBlurTexture.destroy();
		// AUX COLOR
		auxColorsTexture.destroy();
	}

	private static void disposeFrameBuffers() {
		// MODEL
		modelFrameBuffer.destroy();
		// SSAO
		ssaoFrameBuffer.destroy();
		// SSAO BLUR
		ssaoBlurFrameBuffer.destroy();
		// LIGHTING
		lightingFrameBuffer.destroy();
		// ANTI ALIASING
		antiAliasingFrameBuffer.destroy();
	}

	private static void disposeVertexArrays() {
		// UNIT WIRE CUBE
		unitCubeWireVertexArray.destroy();
		// DIAMOND MODEL
		diamondModelVertexArray.destroy();
	}

	public static void setGLVersion(GLVersion version) {
		glVersion = version;
		switch (version) {
			case GL20:
				glFactory = new GL20GLFactory();
				break;
			case GL30:
				glFactory = new GL30GLFactory();
		}
	}

	public static void setCullBackFaces(boolean cull) {
		cullBackFaces = cull;
	}

	public static void setBackgroundColor(Color color) {
		backgroundColor = color;
	}

	public static void setDiffuseIntensity(float intensity) {
		diffuseIntensityUniform.set(intensity);
	}

	public static void setSpecularIntensity(float intensity) {
		specularIntensityUniform.set(intensity);
	}

	public static void setAmbientIntensity(float intensity) {
		ambientIntensityUniform.set(intensity);
	}

	public static void setLightAttenuation(float attenuation) {
		lightAttenuationUniform.set(attenuation);
	}

	public static Color getAABBColor() {
		return aabbModelColor;
	}

	public static void setAABBColor(Color color) {
		aabbModelColor = color;
	}

	public static Color getDiamondColor() {
		return diamondModelColor;
	}

	public static void setDiamondColor(Color color) {
		diamondModelColor = color;
	}

	public static Color getCylinderColor() {
		return cylinderModelColor;
	}

	public static void setCylinderColor(Color color) {
		cylinderModelColor = color;
	}

	public static Color getSphereColor() {
		return sphereModelColor;
	}

	public static void setSphereColor(Color color) {
		sphereModelColor = color;
	}

	public static Camera getCamera() {
		return modelCamera;
	}

	public static void setLightPosition(Vector3 position) {
		lightPositionUniform.set(position);
	}

	public static Model addAABB(Vector3 position, Vector3 size) {
		final Model model = new Model(unitCubeWireVertexArray, solidMaterial);
		model.setPosition(position);
		model.setScale(size);
		model.getUniforms().add(new ColorUniform("modelColor", aabbModelColor));
		modelRenderList.add(model);
		return model;
	}

	public static Model addBox(Vector3 position, Quaternion orientation, Vector3 size) {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(MeshGenerator.generateTexturedCuboid(null, SandboxUtil.toReactVector3(size.mul(2))));
		vertexArray.create();
		final Model model = new Model(vertexArray, woodMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		modelRenderList.add(model);
		return model;
	}

	public static Model addDiamond(Vector3 position, Quaternion orientation) {
		final Model model = new Model(diamondModelVertexArray, solidMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		model.getUniforms().add(new ColorUniform("modelColor", diamondModelColor));
		modelRenderList.add(model);
		return model;
	}

	public static Model addCylinder(Vector3 position, Quaternion orientation, float radius, float height) {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(MeshGenerator.generateCylinder(null, radius, height));
		vertexArray.create();
		final Model model = new Model(vertexArray, solidMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		model.getUniforms().add(new ColorUniform("modelColor", cylinderModelColor));
		modelRenderList.add(model);
		return model;
	}

	public static Model addSphere(Vector3 position, Quaternion orientation, float radius) {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(MeshGenerator.generateSphere(null, radius));
		vertexArray.create();
		final Model model = new Model(vertexArray, solidMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		model.getUniforms().add(new ColorUniform("modelColor", sphereModelColor));
		modelRenderList.add(model);
		return model;
	}

	public static void addModel(Model model) {
		modelRenderList.add(model);
	}

	public static void removeModel(Model model) {
		modelRenderList.remove(model);
	}

	public static void addDefaultObjects() {
		addDeferredStageScreens();
		addCrosshairs();
		addFPSMonitor();
		addCreeper();
	}

	private static void addDeferredStageScreens() {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(MeshGenerator.generateTexturedPlane(null, new Vector2(2, 2)));
		vertexArray.create();
		// SSAO
		final Model ssao = new Model(vertexArray, ssaoMaterial);
		ssaoRenderList.add(ssao);
		// SSAO BLUR
		final Model ssaoBlur = new Model(vertexArray, ssaoBlurMaterial);
		ssaoBlurRenderList.add(ssaoBlur);
		// LIGHTING
		final Model lighting = new Model(vertexArray, lightingMaterial);
		lightingRenderList.add(lighting);
		// ANTI ALIASING
		final Model antiAliasing = new Model(vertexArray, antiAliasingMaterial);
		antiAliasingRenderList.add(antiAliasing);
		// SCREEN
		final Model screen = new Model(vertexArray, screenMaterial);
		guiRenderList.add(screen);
	}

	private static void addCrosshairs() {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(MeshGenerator.generateCrosshairs(null, 0.02f));
		vertexArray.create();
		final Model model = new Model(vertexArray, solidMaterial);
		vertexArray.setDrawingMode(DrawingMode.LINES);
		model.getUniforms().add(new ColorUniform("modelColor", Color.WHITE));
		model.setPosition(new Vector3(0.5, (1 / ASPECT_RATIO) / 2, -0.1));
		guiRenderList.add(model);
	}

	private static void addFPSMonitor() {
		final Font ubuntu;
		try {
			ubuntu = Font.createFont(Font.TRUETYPE_FONT, Sandbox.class.getResourceAsStream("/fonts/ubuntu-r.ttf"));
		} catch (FontFormatException | IOException e) {
			System.out.println(e);
			return;
		}
		final StringModel sandboxModel = new StringModel(glFactory, "SandboxPweryCusticRF0123456789,&: ", ubuntu.deriveFont(Font.PLAIN, 15), WINDOW_SIZE.getFloorX());
		final float aspect = 1 / ASPECT_RATIO;
		sandboxModel.setPosition(new Vector3(0.005, aspect / 2 + 0.315, -0.1));
		final String white = "#ffffffff", brown = "#ffC19953", green = "#ff00ff00", cyan = "#ff4fB5ff";
		sandboxModel.setString(brown + "Sandbox\n" + white + "Powered by " + green + "Caustic" + white + " & " + cyan + "React");
		guiRenderList.add(sandboxModel);
		final InstancedStringModel fpsModel = new InstancedStringModel(sandboxModel);
		fpsModel.setPosition(new Vector3(0.005, aspect / 2 + 0.285, -0.1));
		fpsModel.setString("FPS: " + fpsMonitor.getFPS());
		guiRenderList.add(fpsModel);
		fpsMonitorModel = fpsModel;
	}

	private static void addCreeper() {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(ObjFileLoader.load(Sandbox.class.getResourceAsStream("/models/creeper.obj")));
		vertexArray.create();
		final Model model = new Model(vertexArray, creeperMaterial);
		model.setPosition(new Vector3(10, 10, 0));
		model.setRotation(org.spout.math.imaginary.Quaternion.fromAngleDegAxis(-90, 0, 1, 0));
		modelRenderList.add(model);
		// Add a second mob, instanced from the first one
		final Model instancedMobModel = new InstancedModel(model);
		instancedMobModel.setPosition(new Vector3(-10, 10, 0));
		instancedMobModel.setRotation(org.spout.math.imaginary.Quaternion.fromAngleDegAxis(90, 0, 1, 0));
		modelRenderList.add(instancedMobModel);
	}

	public static void startFPSMonitor() {
		fpsMonitor.start();
	}

	public static void render() {
		renderer.render();
		updateFPSMonitor();
	}

	private static void updateFPSMonitor() {
		fpsMonitor.update();
		fpsMonitorModel.setString("FPS: " + fpsMonitor.getFPS());
	}
}
