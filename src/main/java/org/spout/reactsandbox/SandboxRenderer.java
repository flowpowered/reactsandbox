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
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.spout.math.imaginary.Quaternion;
import org.spout.math.vector.Vector2;
import org.spout.math.vector.Vector3;
import org.spout.renderer.Camera;
import org.spout.renderer.GLVersion;
import org.spout.renderer.data.RenderList;
import org.spout.renderer.data.Uniform.ColorUniform;
import org.spout.renderer.data.Uniform.FloatUniform;
import org.spout.renderer.data.Uniform.Vector3Uniform;
import org.spout.renderer.data.UniformHolder;
import org.spout.renderer.gl.FrameBuffer;
import org.spout.renderer.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.gl.Material;
import org.spout.renderer.gl.Model;
import org.spout.renderer.gl.Program;
import org.spout.renderer.gl.RenderBuffer;
import org.spout.renderer.gl.Renderer;
import org.spout.renderer.gl.Renderer.Capability;
import org.spout.renderer.gl.Shader;
import org.spout.renderer.gl.Shader.ShaderType;
import org.spout.renderer.gl.Texture;
import org.spout.renderer.gl.Texture.FilterMode;
import org.spout.renderer.gl.Texture.ImageFormat;
import org.spout.renderer.gl.VertexArray;
import org.spout.renderer.gl.VertexArray.DrawingMode;
import org.spout.renderer.loader.ObjFileLoader;
import org.spout.renderer.util.InstancedModel;
import org.spout.renderer.util.InstancedStringModel;
import org.spout.renderer.util.StringModel;

/**
 *
 */
public class SandboxRenderer {
	// WINDOW
	private static final String WINDOW_TITLE = "Sandbox";
	private static final Vector2 WINDOW_SIZE = new Vector2(1200, 800);
	private static final float FIELD_OF_VIEW = 75;
	private static final float NEAR_PLANE = 0.001f;
	private static final float FAR_PLANE = 100;
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
	private static final Camera guiCamera = Camera.createOrthographic(1, 0, (float) WINDOW_SIZE.getFloorY() / WINDOW_SIZE.getFloorX(), 0, NEAR_PLANE, FAR_PLANE);
	// OPENGL VERSION
	private static GLVersion glVersion;
	// RENDERER
	private static Renderer renderer;
	// RENDER LISTS
	private static final RenderList modelRenderList = new RenderList("models", modelCamera, 0);
	private static final RenderList transparencyRenderList = new RenderList("transparency", modelCamera, 1);
	private static final RenderList guiRenderList = new RenderList("gui", guiCamera, 2);
	// SHADERS
	private static Shader solidVert;
	private static Shader solidFrag;
	private static Shader wireframeVert;
	private static Shader wireframeFrag;
	private static Shader texturedVert;
	private static Shader texturedFrag;
	private static Shader screenVert;
	private static Shader screenFrag;
	// PROGRAMS
	private static Program solidProgram;
	private static Program wireframeProgram;
	private static Program texturedProgram;
	private static Program screenProgram;
	// TEXTURES
	private static Texture creeperSkinTexture;
	private static Texture woodDiffuseTexture;
	private static Texture woodSpecularTexture;
	private static Texture spoutLogoTexture;
	private static Texture screenTexture;
	// MATERIALS
	private static Material solidMaterial;
	private static Material wireframeMaterial;
	private static Material creeperMaterial;
	private static Material woodMaterial;
	private static Material screenMaterial;
	// RENDER BUFFERS
	private static RenderBuffer depthRenderBuffer;
	// FRAME BUFFERS
	private static FrameBuffer modelFrameBuffer;
	// VERTEX ARRAYS
	private static VertexArray unitCubeWireVertexArray;
	private static VertexArray diamondModelVertexArray;
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
		initRenderBuffers();
		initFrameBuffers();
		initVertexArrays();
	}

	private static void initRenderer() {
		renderer = glVersion.createRenderer();
		renderer.setWindowTitle(WINDOW_TITLE);
		renderer.setWindowSize(WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		renderer.create();
		renderer.setClearColor(backgroundColor);
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
		// TRANSPARENCY
		transparencyRenderList.addCapabilities(Capability.DEPTH_TEST, Capability.BLEND);
		if (glVersion == GLVersion.GL30) {
			transparencyRenderList.addCapability(Capability.DEPTH_CLAMP);
		}
		if (glVersion == GLVersion.GL30) {
			transparencyRenderList.addCapability(Capability.DEPTH_CLAMP);
		}
		renderer.addRenderList(transparencyRenderList);
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
		solidVert = glVersion.createShader();
		solidVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "solid.vert"));
		solidVert.setType(ShaderType.VERTEX);
		solidVert.create();
		// SOLID FRAG
		solidFrag = glVersion.createShader();
		solidFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "solid.frag"));
		solidFrag.setType(ShaderType.FRAGMENT);
		solidFrag.create();
		// WIREFRAME VERT
		wireframeVert = glVersion.createShader();
		wireframeVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "wireframe.vert"));
		wireframeVert.setType(ShaderType.VERTEX);
		wireframeVert.create();
		// WIREFRAME FRAG
		wireframeFrag = glVersion.createShader();
		wireframeFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "wireframe.frag"));
		wireframeFrag.setType(ShaderType.FRAGMENT);
		wireframeFrag.create();
		// TEXTURED VERT
		texturedVert = glVersion.createShader();
		texturedVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "textured.vert"));
		texturedVert.setType(ShaderType.VERTEX);
		texturedVert.create();
		// TEXTURED
		texturedFrag = glVersion.createShader();
		texturedFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "textured.frag"));
		texturedFrag.setType(ShaderType.FRAGMENT);
		texturedFrag.create();
		// SCREEN VERT
		screenVert = glVersion.createShader();
		screenVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "screen.vert"));
		screenVert.setType(ShaderType.VERTEX);
		screenVert.create();
		// SCREEN FRAG
		screenFrag = glVersion.createShader();
		screenFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "screen.frag"));
		screenFrag.setType(ShaderType.FRAGMENT);
		screenFrag.create();
	}

	private static void initPrograms() {
		// SOLID
		solidProgram = glVersion.createProgram();
		solidProgram.addShader(solidVert);
		solidProgram.addShader(solidFrag);
		if (glVersion == GLVersion.GL20) {
			solidProgram.addAttributeLayout("position", 0);
			solidProgram.addAttributeLayout("normal", 1);
		}
		solidProgram.create();
		// WIREFRAME
		wireframeProgram = glVersion.createProgram();
		wireframeProgram.addShader(wireframeVert);
		wireframeProgram.addShader(wireframeFrag);
		if (glVersion == GLVersion.GL20) {
			wireframeProgram.addAttributeLayout("position", 0);
		}
		wireframeProgram.create();
		// TEXTURED
		texturedProgram = glVersion.createProgram();
		texturedProgram.addShader(texturedVert);
		texturedProgram.addShader(texturedFrag);
		if (glVersion == GLVersion.GL20) {
			texturedProgram.addAttributeLayout("position", 0);
			texturedProgram.addAttributeLayout("normal", 1);
			texturedProgram.addAttributeLayout("textureCoords", 2);
		}
		texturedProgram.addTextureLayout("diffuse", 0);
		texturedProgram.addTextureLayout("specular", 1);
		texturedProgram.create();
		// SCREEN
		screenProgram = glVersion.createProgram();
		screenProgram.addShader(screenVert);
		screenProgram.addShader(screenFrag);
		if (glVersion == GLVersion.GL20) {
			screenProgram.addAttributeLayout("position", 0);
			screenProgram.addAttributeLayout("textureCoords", 2);
		}
		screenProgram.addTextureLayout("diffuse", 0);
		screenProgram.create();
	}

	private static void initTextures() {
		// CREEPER SKIN
		creeperSkinTexture = glVersion.createTexture();
		creeperSkinTexture.setImageData(Sandbox.class.getResourceAsStream("/textures/creeper.png"));
		creeperSkinTexture.create();
		// WOOD DIFFUSE
		woodDiffuseTexture = glVersion.createTexture();
		woodDiffuseTexture.setImageData(Sandbox.class.getResourceAsStream("/textures/wood_diffuse.png"));
		woodDiffuseTexture.setMagFilter(FilterMode.LINEAR);
		woodDiffuseTexture.setMinFilter(FilterMode.LINEAR_MIPMAP_LINEAR);
		woodDiffuseTexture.create();
		// WOOD SPECULAR
		woodSpecularTexture = glVersion.createTexture();
		woodDiffuseTexture.setFormat(ImageFormat.RED);
		woodSpecularTexture.setImageData(Sandbox.class.getResourceAsStream("/textures/wood_specular.png"));
		woodSpecularTexture.setMagFilter(FilterMode.LINEAR);
		woodSpecularTexture.setMinFilter(FilterMode.LINEAR_MIPMAP_LINEAR);
		woodSpecularTexture.create();
		// SPOUT LOGO
		spoutLogoTexture = glVersion.createTexture();
		spoutLogoTexture.setFormat(ImageFormat.RGBA);
		spoutLogoTexture.setImageData(Sandbox.class.getResourceAsStream("/textures/spout.png"));
		spoutLogoTexture.create();
		// SCREEN
		screenTexture = glVersion.createTexture();
		screenTexture.setFormat(ImageFormat.RGB);
		screenTexture.setImageData((ByteBuffer) null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		screenTexture.create();
	}

	private static void initMaterials() {
		UniformHolder uniforms;
		// SOLID
		solidMaterial = glVersion.createMaterial();
		solidMaterial.setProgram(solidProgram);
		solidMaterial.create();
		uniforms = solidMaterial.getUniforms();
		uniforms.add(lightPositionUniform);
		uniforms.add(diffuseIntensityUniform);
		uniforms.add(specularIntensityUniform);
		uniforms.add(ambientIntensityUniform);
		uniforms.add(lightAttenuationUniform);
		// WIREFRAME
		wireframeMaterial = glVersion.createMaterial();
		wireframeMaterial.setProgram(wireframeProgram);
		wireframeMaterial.create();
		// CREEPER
		creeperMaterial = glVersion.createMaterial();
		creeperMaterial.setProgram(texturedProgram);
		creeperMaterial.addTexture(0, creeperSkinTexture);
		creeperMaterial.create();
		uniforms = creeperMaterial.getUniforms();
		uniforms.add(lightPositionUniform);
		uniforms.add(diffuseIntensityUniform);
		uniforms.add(specularIntensityUniform);
		uniforms.add(ambientIntensityUniform);
		uniforms.add(lightAttenuationUniform);
		// WOOD
		woodMaterial = glVersion.createMaterial();
		woodMaterial.setProgram(texturedProgram);
		woodMaterial.addTexture(0, woodDiffuseTexture);
		woodMaterial.addTexture(1, woodSpecularTexture);
		woodMaterial.create();
		uniforms = woodMaterial.getUniforms();
		uniforms.add(lightPositionUniform);
		uniforms.add(diffuseIntensityUniform);
		uniforms.add(specularIntensityUniform);
		uniforms.add(ambientIntensityUniform);
		uniforms.add(lightAttenuationUniform);
		// SCREEN
		screenMaterial = glVersion.createMaterial();
		screenMaterial.setProgram(screenProgram);
		screenMaterial.addTexture(0, screenTexture);
		screenMaterial.create();
	}

	private static void initRenderBuffers() {
		// DEPTH
		depthRenderBuffer = glVersion.createRenderBuffer();
		depthRenderBuffer.setFormat(ImageFormat.DEPTH);
		depthRenderBuffer.setSize(WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		depthRenderBuffer.create();
	}

	private static void initFrameBuffers() {
		// MODEL
		modelFrameBuffer = glVersion.createFrameBuffer();
		modelFrameBuffer.attach(AttachmentPoint.COLOR0, screenTexture);
		modelFrameBuffer.attach(AttachmentPoint.DEPTH, depthRenderBuffer);
		modelFrameBuffer.create();
		modelRenderList.setFrameBuffer(modelFrameBuffer);
		transparencyRenderList.setFrameBuffer(modelFrameBuffer);
	}

	private static void initVertexArrays() {
		// UNIT WIRE CUBE
		unitCubeWireVertexArray = glVersion.createVertexArray();
		unitCubeWireVertexArray.setData(MeshGenerator.generateWireCuboid(null, new org.spout.physics.math.Vector3(1, 1, 1)));
		unitCubeWireVertexArray.setDrawingMode(DrawingMode.LINES);
		unitCubeWireVertexArray.create();
		// DIAMOND MODEL
		diamondModelVertexArray = glVersion.createVertexArray();
		diamondModelVertexArray.setData(ObjFileLoader.load(Sandbox.class.getResourceAsStream("/models/diamond.obj")));
		diamondModelVertexArray.create();
	}

	public static void dispose() {
		disposeRenderLists();
		disposeShaders();
		disposePrograms();
		disposeTextures();
		disposeMaterials();
		disposeRenderBuffers();
		disposeFrameBuffers();
		disposeVertexArrays();
		disposeRenderer();
	}

	private static void disposeRenderer() {
		renderer.destroy();
	}

	private static void disposeRenderLists() {
		// MODEL
		modelRenderList.clearCapabilities();
		for (Model model : modelRenderList) {
			model.destroy();
		}
		// TRANSPARENCY
		transparencyRenderList.clearCapabilities();
		for (Model model : transparencyRenderList) {
			model.destroy();
		}
		// GUI
		guiRenderList.clearCapabilities();
		for (Model model : guiRenderList) {
			model.destroy();
		}
	}

	private static void disposeShaders() {
		// SOLID
		solidVert.destroy();
		solidFrag.destroy();
		// WIREFRAME
		wireframeVert.destroy();
		wireframeFrag.destroy();
		// TEXTURED
		texturedVert.destroy();
		texturedFrag.destroy();
		// SCREEN
		screenVert.destroy();
		screenFrag.destroy();
	}

	private static void disposePrograms() {
		// SOLID
		solidProgram.destroy();
		// WIREFRAME
		wireframeProgram.destroy();
		// TEXTURED
		texturedProgram.destroy();
		// SCREEN
		screenProgram.destroy();
	}

	private static void disposeTextures() {
		// CREEPER SKIN
		creeperSkinTexture.destroy();
		// WOOD DIFFUSE
		woodDiffuseTexture.destroy();
		// WOOD SPECULAR
		woodSpecularTexture.destroy();
		// SPOUT LOGO
		spoutLogoTexture.destroy();
		// SCREEN
		screenTexture.destroy();
	}

	private static void disposeMaterials() {
		// SOLID
		solidMaterial.destroy();
		// WIRE
		wireframeMaterial.destroy();
		// CREEPER
		creeperMaterial.destroy();
		// WOOD
		woodMaterial.destroy();
		// SCREEN
		screenMaterial.destroy();
	}

	private static void disposeRenderBuffers() {
		// DEPTH
		depthRenderBuffer.destroy();
	}

	private static void disposeFrameBuffers() {
		// MODEL
		modelFrameBuffer.destroy();
	}

	private static void disposeVertexArrays() {
		// UNIT WIRE CUBE
		unitCubeWireVertexArray.destroy();
		// DIAMOND MODEL
		diamondModelVertexArray.destroy();
	}

	public static void setGLVersion(GLVersion version) {
		glVersion = version;
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
		final Model model = glVersion.createModel();
		model.setVertexArray(unitCubeWireVertexArray);
		model.setMaterial(wireframeMaterial);
		model.setPosition(position);
		model.setScale(size);
		model.create();
		model.getUniforms().add(new ColorUniform("modelColor", aabbModelColor));
		modelRenderList.add(model);
		return model;
	}

	public static Model addBox(Vector3 position, Quaternion orientation, Vector3 size) {
		final Model model = glVersion.createModel();
		final VertexArray vertexArray = glVersion.createVertexArray();
		vertexArray.setData(MeshGenerator.generateTexturedCuboid(null, SandboxUtil.toReactVector3(size.mul(2))));
		vertexArray.create();
		model.setVertexArray(vertexArray);
		model.setMaterial(woodMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		model.create();
		modelRenderList.add(model);
		return model;
	}

	public static Model addDiamond(Vector3 position, Quaternion orientation) {
		final Model model = glVersion.createModel();
		model.setVertexArray(diamondModelVertexArray);
		model.setMaterial(solidMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		model.create();
		model.getUniforms().add(new ColorUniform("modelColor", diamondModelColor));
		modelRenderList.add(model);
		return model;
	}

	public static Model addCylinder(Vector3 position, Quaternion orientation, float radius, float height) {
		final Model model = glVersion.createModel();
		final VertexArray vertexArray = glVersion.createVertexArray();
		vertexArray.setData(MeshGenerator.generateCylinder(null, radius, height));
		vertexArray.create();
		model.setVertexArray(vertexArray);
		model.setMaterial(solidMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		model.create();
		model.getUniforms().add(new ColorUniform("modelColor", cylinderModelColor));
		modelRenderList.add(model);
		return model;
	}

	public static Model addSphere(Vector3 position, Quaternion orientation, float radius) {
		final Model model = glVersion.createModel();
		final VertexArray vertexArray = glVersion.createVertexArray();
		vertexArray.setData(MeshGenerator.generateSphere(null, radius));
		vertexArray.create();
		model.setVertexArray(vertexArray);
		model.setMaterial(solidMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		model.create();
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
		addScreenPlane();
		addCrosshairs();
		addFPSMonitor();
		addCreeper();
		addTransparentPlane();
	}

	private static void addScreenPlane() {
		final Model model = glVersion.createModel();
		final VertexArray vertexArray = glVersion.createVertexArray();
		final float aspect = WINDOW_SIZE.getY() / WINDOW_SIZE.getX();
		vertexArray.setData(MeshGenerator.generateTexturedPlane(null, new Vector2(1, aspect)));
		vertexArray.create();
		model.setVertexArray(vertexArray);
		model.setMaterial(screenMaterial);
		model.setPosition(new Vector3(0.5, aspect / 2, -0.002));
		model.create();
		guiRenderList.add(model);
	}

	private static void addCrosshairs() {
		final Model model = glVersion.createModel();
		final VertexArray vertexArray = glVersion.createVertexArray();
		vertexArray.setData(MeshGenerator.generateCrosshairs(null, 0.02f));
		vertexArray.create();
		model.setVertexArray(vertexArray);
		model.setMaterial(wireframeMaterial);
		vertexArray.setDrawingMode(DrawingMode.LINES);
		model.getUniforms().add(new ColorUniform("modelColor", Color.WHITE));
		model.setPosition(new Vector3(0.5, (WINDOW_SIZE.getY() / WINDOW_SIZE.getX()) / 2, -0.001));
		model.create();
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
		final StringModel sandboxModel = new StringModel();
		sandboxModel.setGLVersion(glVersion);
		sandboxModel.setGlyphs("SandboxPweryCusticRF0123456789,&: ");
		sandboxModel.setFont(ubuntu.deriveFont(Font.PLAIN, 15));
		sandboxModel.setWindowWidth(WINDOW_SIZE.getFloorX());
		sandboxModel.create();
		final float aspect = WINDOW_SIZE.getY() / WINDOW_SIZE.getX();
		sandboxModel.setPosition(new Vector3(0.005, aspect / 2 + 0.315, -0.001));
		final String white = "#ffffffff", brown = "#ffC19953", green = "#ff00ff00", cyan = "#ff4fB5ff";
		sandboxModel.setString(brown + "Sandbox\n" + white + "Powered by " + green + "Caustic" + white + " & " + cyan + "React");
		guiRenderList.add(sandboxModel);
		final InstancedStringModel fpsModel = new InstancedStringModel(sandboxModel);
		fpsModel.create();
		fpsModel.setPosition(new Vector3(0.005, aspect / 2 + 0.285, -0.001));
		fpsModel.setString("FPS: " + fpsMonitor.getFPS());
		guiRenderList.add(fpsModel);
		fpsMonitorModel = fpsModel;
	}

	private static void addCreeper() {
		final Model model = glVersion.createModel();
		final VertexArray vertexArray = glVersion.createVertexArray();
		vertexArray.setData(ObjFileLoader.load(Sandbox.class.getResourceAsStream("/models/creeper.obj")));
		vertexArray.create();
		model.setVertexArray(vertexArray);
		model.setMaterial(creeperMaterial);
		model.setPosition(new Vector3(10, 10, 0));
		model.setRotation(org.spout.math.imaginary.Quaternion.fromAngleDegAxis(-90, 0, 1, 0));
		model.create();
		modelRenderList.add(model);
		// Add a second mob, instanced from the first one
		final Model instancedMobModel = new InstancedModel(model);
		instancedMobModel.create();
		instancedMobModel.setPosition(new Vector3(-10, 10, 0));
		instancedMobModel.setRotation(org.spout.math.imaginary.Quaternion.fromAngleDegAxis(90, 0, 1, 0));
		modelRenderList.add(instancedMobModel);
	}

	private static void addTransparentPlane() {
		final Model model = glVersion.createModel();
		final VertexArray vertexArray = glVersion.createVertexArray();
		vertexArray.setData(MeshGenerator.generateTexturedPlane(null, new Vector2(4, 4)));
		vertexArray.create();
		model.setVertexArray(vertexArray);
		model.setMaterial(solidMaterial);
		model.getUniforms().add(new ColorUniform("modelColor", new Color(1f, 1f, 1f, 0.5f)));
		model.setPosition(new Vector3(0, 10, -10));
		model.create();
		transparencyRenderList.add(model);
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
