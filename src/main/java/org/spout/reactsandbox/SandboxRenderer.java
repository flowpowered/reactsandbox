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

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import org.lwjgl.opengl.GLContext;

import org.spout.math.GenericMath;
import org.spout.math.TrigMath;
import org.spout.math.imaginary.Quaternion;
import org.spout.math.matrix.Matrix4;
import org.spout.math.vector.Vector2;
import org.spout.math.vector.Vector3;
import org.spout.renderer.Action.RenderModelsAction;
import org.spout.renderer.Camera;
import org.spout.renderer.GLImplementation;
import org.spout.renderer.GLVersioned.GLVersion;
import org.spout.renderer.Material;
import org.spout.renderer.Pipeline;
import org.spout.renderer.Pipeline.PipelineBuilder;
import org.spout.renderer.data.Color;
import org.spout.renderer.data.Uniform.ColorUniform;
import org.spout.renderer.data.Uniform.FloatUniform;
import org.spout.renderer.data.Uniform.IntUniform;
import org.spout.renderer.data.Uniform.Matrix4Uniform;
import org.spout.renderer.data.Uniform.Vector2Uniform;
import org.spout.renderer.data.Uniform.Vector3Uniform;
import org.spout.renderer.data.UniformHolder;
import org.spout.renderer.data.VertexAttribute;
import org.spout.renderer.data.VertexAttribute.DataType;
import org.spout.renderer.data.VertexData;
import org.spout.renderer.gl.Context;
import org.spout.renderer.gl.Context.Capability;
import org.spout.renderer.gl.FrameBuffer;
import org.spout.renderer.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.gl.GLFactory;
import org.spout.renderer.gl.Program;
import org.spout.renderer.gl.Shader;
import org.spout.renderer.gl.Texture;
import org.spout.renderer.gl.Texture.CompareMode;
import org.spout.renderer.gl.Texture.FilterMode;
import org.spout.renderer.gl.Texture.Format;
import org.spout.renderer.gl.Texture.InternalFormat;
import org.spout.renderer.gl.Texture.WrapMode;
import org.spout.renderer.gl.VertexArray;
import org.spout.renderer.gl.VertexArray.DrawingMode;
import org.spout.renderer.model.Model;
import org.spout.renderer.model.StringModel;
import org.spout.renderer.util.CausticUtil;
import org.spout.renderer.util.ColladaFileLoader;
import org.spout.renderer.util.ObjFileLoader;
import org.spout.renderer.util.Rectangle;

public class SandboxRenderer {
	// CONSTANTS
	private static final String WINDOW_TITLE = "Sandbox";
	private static final Vector2 WINDOW_SIZE = new Vector2(1200, 800);
	private static final Vector2 SHADOW_SIZE = new Vector2(2048, 2048);
	private static final float ASPECT_RATIO = WINDOW_SIZE.getX() / WINDOW_SIZE.getY();
	private static final float FIELD_OF_VIEW = 60;
	private static final float TAN_HALF_FOV = (float) Math.tan(Math.toRadians(FIELD_OF_VIEW) / 2);
	private static final float NEAR_PLANE = 0.1f;
	private static final float FAR_PLANE = 1000;
	private static final Vector2 PROJECTION = new Vector2(FAR_PLANE / (FAR_PLANE - NEAR_PLANE), (-FAR_PLANE * NEAR_PLANE) / (FAR_PLANE - NEAR_PLANE));
	private static final DateFormat SCREENSHOT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
	// SETTINGS
	private static Color backgroundColor = Color.DARK_GRAY;
	private static boolean cullBackFaces = true;
	// EFFECT UNIFORMS
	private static final Vector3Uniform lightPositionUniform = new Vector3Uniform("lightPosition", Vector3.ZERO);
	private static final Vector3Uniform spotDirectionUniform = new Vector3Uniform("spotDirection", new Vector3(0, 0, -1));
	private static final FloatUniform lightAttenuationUniform = new FloatUniform("lightAttenuation", 0.03f);
	private static final Matrix4Uniform inverseViewMatrixUniform = new Matrix4Uniform("inverseViewMatrix", new Matrix4());
	private static final Matrix4Uniform lightViewMatrixUniform = new Matrix4Uniform("lightViewMatrix", new Matrix4());
	private static final Matrix4Uniform lightProjectionMatrixUniform = new Matrix4Uniform("lightProjectionMatrix", new Matrix4());
	private static final Matrix4Uniform previousViewMatrixUniform = new Matrix4Uniform("previousViewMatrix", new Matrix4());
	private static final Matrix4Uniform previousProjectionMatrixUniform = new Matrix4Uniform("previousProjectionMatrix", new Matrix4());
	private static final FloatUniform blurStrengthUniform = new FloatUniform("blurStrength", 1);
	// CAMERAS
	private static final Camera modelCamera = Camera.createPerspective(FIELD_OF_VIEW, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), NEAR_PLANE, FAR_PLANE);
	private static final Camera lightCamera = Camera.createPerspective((float) TrigMath.RAD_TO_DEG * Sandbox.SPOT_CUTOFF * 2, 1, 1, 0.1f, (float) GenericMath.length(50d, 100d));
	private static final Camera guiCamera = Camera.createOrthographic(1, 0, 1 / ASPECT_RATIO, 0, NEAR_PLANE, FAR_PLANE);
	// OPENGL VERSION AND FACTORY
	private static GLVersion glVersion;
	private static GLFactory glFactory;
	// CONTEXT
	private static Context context;
	// RENDER LISTS
	private static final List<Model> modelRenderList = new ArrayList<>();
	private static final List<Model> guiRenderList = new ArrayList<>();
	// PIPELINE
	private static Pipeline pipeline;
	// SHADERS
	private static Shader solidVert;
	private static Shader solidFrag;
	private static Shader texturedVert;
	private static Shader texturedFrag;
	private static Shader fontVert;
	private static Shader fontFrag;
	private static Shader ssaoVert;
	private static Shader ssaoFrag;
	private static Shader blurVert;
	private static Shader blurFrag;
	private static Shader shadowVert;
	private static Shader shadowFrag;
	private static Shader lightingVert;
	private static Shader lightingFrag;
	private static Shader motionBlurVert;
	private static Shader motionBlurFrag;
	private static Shader antiAliasingVert;
	private static Shader antiAliasingFrag;
	private static Shader screenVert;
	private static Shader screenFrag;
	// PROGRAMS
	private static Program solidProgram;
	private static Program texturedProgram;
	private static Program fontProgram;
	private static Program ssaoProgram;
	private static Program blurProgram;
	private static Program shadowProgram;
	private static Program lightingProgram;
	private static Program motionBlurProgram;
	private static Program antiAliasingProgram;
	private static Program screenProgram;
	// TEXTURES
	private static Texture creeperDiffuseTexture;
	private static Texture creeperNormalsTexture;
	private static Texture creeperSpecularTexture;
	private static Texture woodDiffuseTexture;
	private static Texture woodNormalsTexture;
	private static Texture woodSpecularTexture;
	private static Texture colorsTexture;
	private static Texture normalsTexture;
	private static Texture vertexNormals;
	private static Texture materialsTexture;
	private static Texture velocitiesTexture;
	private static Texture depthsTexture;
	private static Texture lightDepthsTexture;
	private static Texture ssaoTexture;
	private static Texture shadowTexture;
	private static Texture auxRTexture;
	private static Texture auxRGBATexture;
	// MATERIALS
	private static Material solidMaterial;
	private static Material wireframeMaterial;
	private static Material creeperMaterial;
	private static Material woodMaterial;
	private static Material ssaoMaterial;
	private static Material blurMaterial;
	private static Material shadowMaterial;
	private static Material lightingMaterial;
	private static Material motionBlurMaterial;
	private static Material antiAliasingMaterial;
	private static Material screenMaterial;
	// FRAME BUFFERS
	private static FrameBuffer modelFrameBuffer;
	private static FrameBuffer lightModelFrameBuffer;
	private static FrameBuffer ssaoFrameBuffer;
	private static FrameBuffer blurFrameBuffer;
	private static FrameBuffer shadowFrameBuffer;
	private static FrameBuffer lightingFrameBuffer;
	private static FrameBuffer motionBlurFrameBuffer;
	private static FrameBuffer antiAliasingFrameBuffer;
	// VERTEX ARRAYS
	private static VertexArray unitCubeWireVertexArray;
	private static VertexArray diamondModelVertexArray;
	private static VertexArray deferredStageScreenVertexArray;
	// EFFECTS
	private static SSAOEffect ssaoEffect;
	private static ShadowMappingEffect shadowMappingEffect;
	private static BlurEffect blurEffect;
	// MODEL PROPERTIES
	private static Color aabbModelColor;
	private static Color diamondModelColor;
	private static Color cylinderModelColor;
	private static Color sphereModelColor;
	// MODELS
	private static Model movingMobModel;
	// FPS MONITOR
	private static final FPSMonitor fpsMonitor = new FPSMonitor();
	private static StringModel fpsMonitorModel;

	public static void init() {
		initContext();
		initEffects();
		initShaders();
		initPrograms();
		initTextures();
		initMaterials();
		initFrameBuffers();
		initVertexArrays();
		initPipeline();
	}

	private static void initContext() {
		// CONTEXT
		context = glFactory.createContext();
		context.setWindowTitle(WINDOW_TITLE);
		context.setWindowSize(WINDOW_SIZE);
		context.create();
		context.setClearColor(new Color(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), 0));
		context.setCamera(modelCamera);
		if (cullBackFaces) {
			context.enableCapability(Capability.CULL_FACE);
		}
		context.enableCapability(Capability.DEPTH_TEST);
		if (glVersion == GLVersion.GL30 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
			context.enableCapability(Capability.DEPTH_CLAMP);
		}
		final UniformHolder uniforms = context.getUniforms();
		uniforms.add(previousViewMatrixUniform);
		uniforms.add(previousProjectionMatrixUniform);
	}

	private static void initEffects() {
		final int blurSize = 2;
		// SSAO
		ssaoEffect = new SSAOEffect(glFactory, WINDOW_SIZE, 8, blurSize, 0.5f, 0.15f, 2);
		// SHADOW MAPPING
		shadowMappingEffect = new ShadowMappingEffect(glFactory, WINDOW_SIZE, 8, blurSize, 0.000006f, 0.0004f);
		// BLUR
		blurEffect = new BlurEffect(WINDOW_SIZE, blurSize);
	}

	private static void initPipeline() {
		PipelineBuilder pipelineBuilder = new PipelineBuilder();
		// MODEL
		pipelineBuilder = pipelineBuilder.bindFrameBuffer(modelFrameBuffer).clearBuffer().renderModels(modelRenderList).unbindFrameBuffer(modelFrameBuffer);
		// LIGHT MODEL
		pipelineBuilder = pipelineBuilder.useViewPort(new Rectangle(Vector2.ZERO, SHADOW_SIZE)).useCamera(lightCamera).bindFrameBuffer(lightModelFrameBuffer).clearBuffer()
				.renderModels(modelRenderList).unbindFrameBuffer(lightModelFrameBuffer).useViewPort(new Rectangle(Vector2.ZERO, WINDOW_SIZE)).useCamera(modelCamera);
		// SSAO
		if (glVersion == GLVersion.GL30 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
			pipelineBuilder = pipelineBuilder.disableCapabilities(Capability.DEPTH_CLAMP);
		}
		pipelineBuilder = pipelineBuilder.disableCapabilities(Capability.DEPTH_TEST).doAction(new DoDeferredStageAction(ssaoFrameBuffer, deferredStageScreenVertexArray, ssaoMaterial));
		// SHADOW
		pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(shadowFrameBuffer, deferredStageScreenVertexArray, shadowMaterial));
		// BLUR
		pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(blurFrameBuffer, deferredStageScreenVertexArray, blurMaterial));
		// LIGHTING
		pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(lightingFrameBuffer, deferredStageScreenVertexArray, lightingMaterial));
		// MOTION BLUR
		pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(motionBlurFrameBuffer, deferredStageScreenVertexArray, motionBlurMaterial));
		// ANTI ALIASING
		pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(antiAliasingFrameBuffer, deferredStageScreenVertexArray, antiAliasingMaterial))
				.unbindFrameBuffer(antiAliasingFrameBuffer).enableCapabilities(Capability.DEPTH_TEST);
		if (glVersion == GLVersion.GL30 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
			pipelineBuilder = pipelineBuilder.enableCapabilities(Capability.DEPTH_CLAMP);
		}
		// GUI
		pipelineBuilder = pipelineBuilder.useCamera(guiCamera).clearBuffer().renderModels(guiRenderList).useCamera(modelCamera).updateDisplay();
		pipeline = pipelineBuilder.build();
	}

	private static void initShaders() {
		final String shaderPath = "/shaders/" + glVersion.toString().toLowerCase() + "/";
		// SOLID VERT
		solidVert = glFactory.createShader();
		solidVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "solid.vert"));
		solidVert.create();
		// SOLID FRAG
		solidFrag = glFactory.createShader();
		solidFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "solid.frag"));
		solidFrag.create();
		// TEXTURED VERT
		texturedVert = glFactory.createShader();
		texturedVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "textured.vert"));
		texturedVert.create();
		// TEXTURED FRAG
		texturedFrag = glFactory.createShader();
		texturedFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "textured.frag"));
		texturedFrag.create();
		// FONT VERT
		fontVert = glFactory.createShader();
		fontVert.setSource(StringModel.class.getResourceAsStream(shaderPath + "font.vert"));
		fontVert.create();
		// FONT FRAG
		fontFrag = glFactory.createShader();
		fontFrag.setSource(StringModel.class.getResourceAsStream(shaderPath + "font.frag"));
		fontFrag.create();
		// SSAO VERT
		ssaoVert = glFactory.createShader();
		ssaoVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "ssao.vert"));
		ssaoVert.create();
		// SSAO FRAG
		ssaoFrag = glFactory.createShader();
		ssaoFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "ssao.frag"));
		ssaoFrag.create();
		// SHADOW VERT
		shadowVert = glFactory.createShader();
		shadowVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "shadow.vert"));
		shadowVert.create();
		// SHADOW FRAG
		shadowFrag = glFactory.createShader();
		shadowFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "shadow.frag"));
		shadowFrag.create();
		// BLUR VERT
		blurVert = glFactory.createShader();
		blurVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "blur.vert"));
		blurVert.create();
		// BLUR FRAG
		blurFrag = glFactory.createShader();
		blurFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "blur.frag"));
		blurFrag.create();
		// LIGHTING VERT
		lightingVert = glFactory.createShader();
		lightingVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "lighting.vert"));
		lightingVert.create();
		// LIGHTING FRAG
		lightingFrag = glFactory.createShader();
		lightingFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "lighting.frag"));
		lightingFrag.create();
		// MOTION BLUR VERT
		motionBlurVert = glFactory.createShader();
		motionBlurVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "motionBlur.vert"));
		motionBlurVert.create();
		// MOTION BLUR FRAG
		motionBlurFrag = glFactory.createShader();
		motionBlurFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "motionBlur.frag"));
		motionBlurFrag.create();
		// ANTI ALIASING VERT
		antiAliasingVert = glFactory.createShader();
		antiAliasingVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "edaa.vert"));
		antiAliasingVert.create();
		// ANTI ALIASING FRAG
		antiAliasingFrag = glFactory.createShader();
		antiAliasingFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "edaa.frag"));
		antiAliasingFrag.create();
		// SCREEN VERT
		screenVert = glFactory.createShader();
		screenVert.setSource(Sandbox.class.getResourceAsStream(shaderPath + "screen.vert"));
		screenVert.create();
		// SCREEN FRAG
		screenFrag = glFactory.createShader();
		screenFrag.setSource(Sandbox.class.getResourceAsStream(shaderPath + "screen.frag"));
		screenFrag.create();
	}

	private static void initPrograms() {
		// SOLID
		solidProgram = glFactory.createProgram();
		solidProgram.addShader(solidVert);
		solidProgram.addShader(solidFrag);
		solidProgram.create();
		// TEXTURED
		texturedProgram = glFactory.createProgram();
		texturedProgram.addShader(texturedVert);
		texturedProgram.addShader(texturedFrag);
		texturedProgram.create();
		/// FONT
		fontProgram = glFactory.createProgram();
		fontProgram.addShader(fontVert);
		fontProgram.addShader(fontFrag);
		fontProgram.create();
		// SSAO
		ssaoProgram = glFactory.createProgram();
		ssaoProgram.addShader(ssaoVert);
		ssaoProgram.addShader(ssaoFrag);
		ssaoProgram.create();
		// SHADOW
		shadowProgram = glFactory.createProgram();
		shadowProgram.addShader(shadowVert);
		shadowProgram.addShader(shadowFrag);
		shadowProgram.create();
		// BLUR
		blurProgram = glFactory.createProgram();
		blurProgram.addShader(blurVert);
		blurProgram.addShader(blurFrag);
		blurProgram.create();
		// LIGHTING
		lightingProgram = glFactory.createProgram();
		lightingProgram.addShader(lightingVert);
		lightingProgram.addShader(lightingFrag);
		lightingProgram.create();
		// MOTION BLUR
		motionBlurProgram = glFactory.createProgram();
		motionBlurProgram.addShader(motionBlurVert);
		motionBlurProgram.addShader(motionBlurFrag);
		motionBlurProgram.create();
		// ANTI ALIASING
		antiAliasingProgram = glFactory.createProgram();
		antiAliasingProgram.addShader(antiAliasingVert);
		antiAliasingProgram.addShader(antiAliasingFrag);
		antiAliasingProgram.create();
		// SCREEN
		screenProgram = glFactory.createProgram();
		screenProgram.addShader(screenVert);
		screenProgram.addShader(screenFrag);
		screenProgram.create();
	}

	private static void initTextures() {
		ByteBuffer data;
		final Rectangle size = new Rectangle();
		// CREEPER DIFFUSE
		creeperDiffuseTexture = glFactory.createTexture();
		data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/creeper_diffuse.png"), Format.RGB, size);
		data.flip();
		creeperDiffuseTexture.setImageData(data, size.getWidth(), size.getHeight());
		creeperDiffuseTexture.create();
		// CREEPER NORMALS
		creeperNormalsTexture = glFactory.createTexture();
		data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/creeper_normals.png"), Format.RGB, size);
		data.flip();
		creeperNormalsTexture.setImageData(data, size.getWidth(), size.getHeight());
		creeperNormalsTexture.setMagFilter(FilterMode.LINEAR);
		creeperNormalsTexture.setMinFilter(FilterMode.LINEAR);
		creeperNormalsTexture.create();
		// CREEPER SPECULAR
		creeperSpecularTexture = glFactory.createTexture();
		creeperSpecularTexture.setFormat(Format.RED);
		creeperSpecularTexture.setInternalFormat(InternalFormat.R8);
		data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/creeper_specular.png"), Format.RED, size);
		data.flip();
		creeperSpecularTexture.setImageData(data, size.getWidth(), size.getHeight());
		creeperSpecularTexture.setMagFilter(FilterMode.LINEAR);
		creeperSpecularTexture.setMinFilter(FilterMode.LINEAR);
		creeperSpecularTexture.create();
		// WOOD DIFFUSE
		woodDiffuseTexture = glFactory.createTexture();
		data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/wood_diffuse.png"), Format.RGB, size);
		data.flip();
		woodDiffuseTexture.setImageData(data, size.getWidth(), size.getHeight());
		woodDiffuseTexture.setMagFilter(FilterMode.LINEAR);
		woodDiffuseTexture.setMinFilter(FilterMode.LINEAR_MIPMAP_LINEAR);
		woodDiffuseTexture.setAnisotropicFiltering(16);
		woodDiffuseTexture.create();
		// WOOD NORMALS
		woodNormalsTexture = glFactory.createTexture();
		data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/wood_normals.png"), Format.RGB, size);
		data.flip();
		woodNormalsTexture.setImageData(data, size.getWidth(), size.getHeight());
		woodNormalsTexture.setMagFilter(FilterMode.LINEAR);
		woodNormalsTexture.setMinFilter(FilterMode.LINEAR_MIPMAP_LINEAR);
		woodNormalsTexture.setAnisotropicFiltering(16);
		woodNormalsTexture.create();
		// WOOD SPECULAR
		woodSpecularTexture = glFactory.createTexture();
		woodSpecularTexture.setFormat(Format.RED);
		woodSpecularTexture.setInternalFormat(InternalFormat.R8);
		data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/wood_specular.png"), Format.RED, size);
		data.flip();
		woodSpecularTexture.setImageData(data, size.getWidth(), size.getHeight());
		woodSpecularTexture.setMagFilter(FilterMode.LINEAR);
		woodSpecularTexture.setMinFilter(FilterMode.LINEAR_MIPMAP_LINEAR);
		woodSpecularTexture.setAnisotropicFiltering(16);
		woodSpecularTexture.create();
		// COLORS
		colorsTexture = glFactory.createTexture();
		colorsTexture.setFormat(Format.RGBA);
		colorsTexture.setInternalFormat(InternalFormat.RGBA8);
		colorsTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		colorsTexture.setMagFilter(FilterMode.LINEAR);
		colorsTexture.setMinFilter(FilterMode.LINEAR);
		colorsTexture.create();
		// NORMALS
		normalsTexture = glFactory.createTexture();
		normalsTexture.setFormat(Format.RGBA);
		normalsTexture.setInternalFormat(InternalFormat.RGBA8);
		normalsTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		normalsTexture.create();
		// VERTEX NORMALS
		vertexNormals = glFactory.createTexture();
		vertexNormals.setFormat(Format.RGBA);
		vertexNormals.setInternalFormat(InternalFormat.RGBA8);
		vertexNormals.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		vertexNormals.create();
		// MATERIALS
		materialsTexture = glFactory.createTexture();
		materialsTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		materialsTexture.create();
		// VELOCITIES
		velocitiesTexture = glFactory.createTexture();
		velocitiesTexture.setFormat(Format.RG);
		velocitiesTexture.setInternalFormat(InternalFormat.RG16F);
		velocitiesTexture.setComponentType(DataType.HALF_FLOAT);
		velocitiesTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		velocitiesTexture.create();
		// DEPTHS
		depthsTexture = glFactory.createTexture();
		depthsTexture.setFormat(Format.DEPTH);
		depthsTexture.setInternalFormat(InternalFormat.DEPTH_COMPONENT32);
		depthsTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		depthsTexture.setWrapS(WrapMode.CLAMP_TO_EDGE);
		depthsTexture.setWrapT(WrapMode.CLAMP_TO_EDGE);
		depthsTexture.create();
		// LIGHT DEPTHS
		lightDepthsTexture = glFactory.createTexture();
		lightDepthsTexture.setFormat(Format.DEPTH);
		lightDepthsTexture.setInternalFormat(InternalFormat.DEPTH_COMPONENT32);
		lightDepthsTexture.setImageData(null, SHADOW_SIZE.getFloorX(), SHADOW_SIZE.getFloorY());
		lightDepthsTexture.setWrapS(WrapMode.CLAMP_TO_BORDER);
		lightDepthsTexture.setWrapT(WrapMode.CLAMP_TO_BORDER);
		lightDepthsTexture.setMagFilter(FilterMode.LINEAR);
		lightDepthsTexture.setMinFilter(FilterMode.LINEAR);
		lightDepthsTexture.setCompareMode(CompareMode.LESS);
		lightDepthsTexture.create();
		// SSAO
		ssaoTexture = glFactory.createTexture();
		ssaoTexture.setFormat(Format.RED);
		ssaoTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		ssaoTexture.create();
		// SHADOW
		shadowTexture = glFactory.createTexture();
		shadowTexture.setFormat(Format.RED);
		shadowTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		shadowTexture.create();
		// AUX R
		auxRTexture = glFactory.createTexture();
		auxRTexture.setFormat(Format.RED);
		auxRTexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		auxRTexture.create();
		// AUX RGBA
		auxRGBATexture = glFactory.createTexture();
		auxRGBATexture.setFormat(Format.RGBA);
		auxRGBATexture.setInternalFormat(InternalFormat.RGBA8);
		auxRGBATexture.setImageData(null, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY());
		auxRGBATexture.setWrapS(WrapMode.CLAMP_TO_EDGE);
		auxRGBATexture.setWrapT(WrapMode.CLAMP_TO_EDGE);
		auxRGBATexture.setMagFilter(FilterMode.LINEAR);
		auxRGBATexture.setMinFilter(FilterMode.LINEAR);
		auxRGBATexture.create();
	}

	private static void initMaterials() {
		UniformHolder uniforms;
		// SOLID
		solidMaterial = new Material(solidProgram);
		uniforms = solidMaterial.getUniforms();
		uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
		uniforms.add(new FloatUniform("specularIntensity", 1));
		uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
		// WIREFRAME
		wireframeMaterial = new Material(solidProgram);
		uniforms = wireframeMaterial.getUniforms();
		uniforms.add(new FloatUniform("diffuseIntensity", 0));
		uniforms.add(new FloatUniform("specularIntensity", 0));
		uniforms.add(new FloatUniform("ambientIntensity", 1));
		// CREEPER
		creeperMaterial = new Material(texturedProgram);
		creeperMaterial.addTexture(0, creeperDiffuseTexture);
		creeperMaterial.addTexture(1, creeperNormalsTexture);
		creeperMaterial.addTexture(2, creeperSpecularTexture);
		uniforms = creeperMaterial.getUniforms();
		uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
		uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
		// WOOD
		woodMaterial = new Material(texturedProgram);
		woodMaterial.addTexture(0, woodDiffuseTexture);
		woodMaterial.addTexture(1, woodNormalsTexture);
		woodMaterial.addTexture(2, woodSpecularTexture);
		uniforms = woodMaterial.getUniforms();
		uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
		uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
		// SSAO
		ssaoMaterial = new Material(ssaoProgram);
		ssaoMaterial.addTexture(0, normalsTexture);
		ssaoMaterial.addTexture(1, depthsTexture);
		ssaoMaterial.addTexture(2, ssaoEffect.getNoiseTexture());
		uniforms = ssaoMaterial.getUniforms();
		uniforms.add(new Vector2Uniform("projection", PROJECTION));
		uniforms.add(new FloatUniform("tanHalfFOV", TAN_HALF_FOV));
		uniforms.add(new FloatUniform("aspectRatio", ASPECT_RATIO));
		ssaoEffect.addUniforms(uniforms);
		// SHADOW
		shadowMaterial = new Material(shadowProgram);
		shadowMaterial.addTexture(0, vertexNormals);
		shadowMaterial.addTexture(1, depthsTexture);
		shadowMaterial.addTexture(2, lightDepthsTexture);
		shadowMaterial.addTexture(3, shadowMappingEffect.getNoiseTexture());
		uniforms = shadowMaterial.getUniforms();
		uniforms.add(new Vector2Uniform("projection", PROJECTION));
		uniforms.add(new FloatUniform("tanHalfFOV", TAN_HALF_FOV));
		uniforms.add(new FloatUniform("aspectRatio", ASPECT_RATIO));
		uniforms.add(lightPositionUniform);
		uniforms.add(inverseViewMatrixUniform);
		uniforms.add(lightViewMatrixUniform);
		uniforms.add(lightProjectionMatrixUniform);
		shadowMappingEffect.addUniforms(uniforms);
		// BLUR
		blurMaterial = new Material(blurProgram);
		blurMaterial.addTexture(0, auxRTexture);
		blurMaterial.addTexture(1, auxRGBATexture);
		uniforms = blurMaterial.getUniforms();
		blurEffect.addUniforms(uniforms);
		// LIGHTING
		lightingMaterial = new Material(lightingProgram);
		lightingMaterial.addTexture(0, colorsTexture);
		lightingMaterial.addTexture(1, normalsTexture);
		lightingMaterial.addTexture(2, depthsTexture);
		lightingMaterial.addTexture(3, materialsTexture);
		lightingMaterial.addTexture(4, ssaoTexture);
		lightingMaterial.addTexture(5, shadowTexture);
		uniforms = lightingMaterial.getUniforms();
		uniforms.add(new Vector2Uniform("projection", PROJECTION));
		uniforms.add(new FloatUniform("tanHalfFOV", TAN_HALF_FOV));
		uniforms.add(new FloatUniform("aspectRatio", ASPECT_RATIO));
		uniforms.add(lightPositionUniform);
		uniforms.add(lightAttenuationUniform);
		uniforms.add(new FloatUniform("spotCutoff", TrigMath.cos(Sandbox.SPOT_CUTOFF)));
		uniforms.add(spotDirectionUniform);
		// MOTION BLUR
		motionBlurMaterial = new Material(motionBlurProgram);
		motionBlurMaterial.addTexture(0, auxRGBATexture);
		motionBlurMaterial.addTexture(1, velocitiesTexture);
		uniforms = motionBlurMaterial.getUniforms();
		uniforms.add(new Vector2Uniform("resolution", WINDOW_SIZE));
		uniforms.add(new IntUniform("sampleCount", 8));
		uniforms.add(blurStrengthUniform);
		// ANTI ALIASING
		antiAliasingMaterial = new Material(antiAliasingProgram);
		antiAliasingMaterial.addTexture(0, colorsTexture);
		antiAliasingMaterial.addTexture(1, vertexNormals);
		antiAliasingMaterial.addTexture(2, depthsTexture);
		uniforms = antiAliasingMaterial.getUniforms();
		uniforms.add(new Vector2Uniform("projection", PROJECTION));
		uniforms.add(new Vector2Uniform("resolution", WINDOW_SIZE));
		uniforms.add(new FloatUniform("maxSpan", 8));
		uniforms.add(new Vector2Uniform("barriers", new Vector2(0.8f, 0.5f)));
		uniforms.add(new Vector2Uniform("weights", new Vector2(0.25f, 0.6f)));
		uniforms.add(new FloatUniform("kernel", 0.75f));
		// SCREEN
		screenMaterial = new Material(screenProgram);
		screenMaterial.addTexture(0, auxRGBATexture);
	}

	private static void initFrameBuffers() {
		// MODEL
		modelFrameBuffer = glFactory.createFrameBuffer();
		modelFrameBuffer.attach(AttachmentPoint.COLOR0, colorsTexture);
		modelFrameBuffer.attach(AttachmentPoint.COLOR1, normalsTexture);
		modelFrameBuffer.attach(AttachmentPoint.COLOR2, vertexNormals);
		modelFrameBuffer.attach(AttachmentPoint.COLOR3, materialsTexture);
		modelFrameBuffer.attach(AttachmentPoint.COLOR4, velocitiesTexture);
		modelFrameBuffer.attach(AttachmentPoint.DEPTH, depthsTexture);
		modelFrameBuffer.create();
		// LIGHT MODEL
		lightModelFrameBuffer = glFactory.createFrameBuffer();
		lightModelFrameBuffer.attach(AttachmentPoint.DEPTH, lightDepthsTexture);
		lightModelFrameBuffer.create();
		// SSAO
		ssaoFrameBuffer = glFactory.createFrameBuffer();
		ssaoFrameBuffer.attach(AttachmentPoint.COLOR0, auxRTexture);
		ssaoFrameBuffer.create();
		// SHADOW
		shadowFrameBuffer = glFactory.createFrameBuffer();
		shadowFrameBuffer.attach(AttachmentPoint.COLOR0, auxRGBATexture);
		shadowFrameBuffer.create();
		// BLUR
		blurFrameBuffer = glFactory.createFrameBuffer();
		blurFrameBuffer.attach(AttachmentPoint.COLOR0, ssaoTexture);
		blurFrameBuffer.attach(AttachmentPoint.COLOR1, shadowTexture);
		blurFrameBuffer.create();
		// LIGHTING
		lightingFrameBuffer = glFactory.createFrameBuffer();
		lightingFrameBuffer.attach(AttachmentPoint.COLOR0, auxRGBATexture);
		lightingFrameBuffer.create();
		// MOTION BLUR
		motionBlurFrameBuffer = glFactory.createFrameBuffer();
		motionBlurFrameBuffer.attach(AttachmentPoint.COLOR0, colorsTexture);
		motionBlurFrameBuffer.create();
		// ANTI ALIASING
		antiAliasingFrameBuffer = glFactory.createFrameBuffer();
		antiAliasingFrameBuffer.attach(AttachmentPoint.COLOR0, auxRGBATexture);
		antiAliasingFrameBuffer.create();
	}

	private static void initVertexArrays() {
		// UNIT WIRE CUBE
		unitCubeWireVertexArray = glFactory.createVertexArray();
		unitCubeWireVertexArray.setData(MeshGenerator.generateWireCuboid(null, new org.spout.physics.math.Vector3(1, 1, 1)));
		unitCubeWireVertexArray.setDrawingMode(DrawingMode.LINES);
		unitCubeWireVertexArray.create();
		// DIAMOND MODEL
		diamondModelVertexArray = glFactory.createVertexArray();
		diamondModelVertexArray.setData(loadOBJ(Sandbox.class.getResourceAsStream("/models/diamond.obj")));
		diamondModelVertexArray.create();
		// DEFERRED STAGE SCREEN
		deferredStageScreenVertexArray = glFactory.createVertexArray();
		deferredStageScreenVertexArray.setData(MeshGenerator.generateTexturedPlane(null, new Vector2(2, 2)));
		deferredStageScreenVertexArray.create();
	}

	public static void dispose() {
		disposeEffects();
		disposeShaders();
		disposePrograms();
		disposeTextures();
		disposeFrameBuffers();
		disposeVertexArrays();
		disposeContext();
	}

	private static void disposeContext() {
		// CONTEXT
		context.destroy();
	}

	private static void disposeEffects() {
		// SSAO
		ssaoEffect.dispose();
		// SHADOW MAPPING
		shadowMappingEffect.dispose();
		// BLUR
		blurEffect.dispose();
	}

	private static void disposeShaders() {
		// SOLID
		solidVert.destroy();
		solidFrag.destroy();
		// TEXTURED
		texturedVert.destroy();
		texturedFrag.destroy();
		// FONT
		fontVert.destroy();
		fontFrag.destroy();
		// SSAO
		ssaoVert.destroy();
		ssaoFrag.destroy();
		// SHADOW
		shadowVert.destroy();
		shadowFrag.destroy();
		// BLUR
		blurVert.destroy();
		blurFrag.destroy();
		// LIGHTING
		lightingVert.destroy();
		lightingFrag.destroy();
		// MOTION BLUR
		motionBlurVert.destroy();
		motionBlurFrag.destroy();
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
		// FONT
		fontProgram.destroy();
		// SSAO
		ssaoProgram.destroy();
		// SHADOW
		shadowProgram.destroy();
		// BLUR
		blurProgram.destroy();
		// LIGHTING
		lightingProgram.destroy();
		// MOTION BLUR
		motionBlurProgram.destroy();
		// ANTI ALIASING
		antiAliasingProgram.destroy();
		// SCREEN
		screenProgram.destroy();
	}

	private static void disposeTextures() {
		// CREEPER DIFFUSE
		creeperDiffuseTexture.destroy();
		// CREEPER NORMALS
		creeperNormalsTexture.destroy();
		// CREEPER SPECULAR
		creeperSpecularTexture.destroy();
		// WOOD DIFFUSE
		woodDiffuseTexture.destroy();
		// WOOD NORMALS
		woodNormalsTexture.destroy();
		// WOOD SPECULAR
		woodSpecularTexture.destroy();
		// COLOR
		colorsTexture.destroy();
		// NORMALS
		normalsTexture.destroy();
		// VERTEX NORMALS
		vertexNormals.destroy();
		// MATERIALS
		materialsTexture.destroy();
		// VELOCITIES
		velocitiesTexture.destroy();
		// DEPTHS
		depthsTexture.destroy();
		// LIGHT DEPTHS
		lightDepthsTexture.destroy();
		// SSAO
		ssaoTexture.destroy();
		// SHADOW
		shadowTexture.destroy();
		// AUX R
		auxRTexture.destroy();
		// AUX RGB
		auxRGBATexture.destroy();
	}

	private static void disposeFrameBuffers() {
		// MODEL
		modelFrameBuffer.destroy();
		// SHADOW
		lightModelFrameBuffer.destroy();
		// SSAO
		ssaoFrameBuffer.destroy();
		// SHADOW
		shadowFrameBuffer.destroy();
		// BLUR
		blurFrameBuffer.destroy();
		// LIGHTING
		lightingFrameBuffer.destroy();
		// MOTION BLUR
		motionBlurFrameBuffer.destroy();
		// ANTI ALIASING
		antiAliasingFrameBuffer.destroy();
	}

	private static void disposeVertexArrays() {
		// UNIT WIRE CUBE
		unitCubeWireVertexArray.destroy();
		// DIAMOND MODEL
		diamondModelVertexArray.destroy();
		// DEFERRED STAGE SCREEN
		deferredStageScreenVertexArray.destroy();
	}

	public static void setGLVersion(GLVersion version) {
		glVersion = version;
		glFactory = GLImplementation.get(version);
	}

	public static void setCullBackFaces(boolean cull) {
		cullBackFaces = cull;
	}

	public static void setBackgroundColor(Color color) {
		backgroundColor = color;
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
		lightCamera.setPosition(position);
	}

	public static void setLightDirection(Vector3 direction) {
		direction = direction.normalize();
		spotDirectionUniform.set(direction);
		lightCamera.setRotation(Quaternion.fromRotationTo(Vector3.FORWARD.negate(), direction));
	}

	public static Model addAABB(Vector3 position, Vector3 size) {
		final Model model = new Model(unitCubeWireVertexArray, wireframeMaterial);
		model.setPosition(position);
		model.setScale(size);
		model.getUniforms().add(new ColorUniform("modelColor", aabbModelColor));
		addModel(model);
		return model;
	}

	public static Model addBox(Vector3 position, Quaternion orientation, Vector3 size) {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(MeshGenerator.generateCuboid(null, SandboxUtil.toReactVector3(size.mul(2))));
		vertexArray.create();
		final Model model = new Model(vertexArray, woodMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		addModel(model);
		return model;
	}

	public static Model addDiamond(Vector3 position, Quaternion orientation) {
		final Model model = new Model(diamondModelVertexArray, solidMaterial);
		model.setPosition(position);
		model.setRotation(orientation);
		model.getUniforms().add(new ColorUniform("modelColor", diamondModelColor));
		addModel(model);
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
		addModel(model);
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
		addModel(model);
		return model;
	}

	public static void addModel(Model model) {
		model.getUniforms().add(new Matrix4Uniform("previousModelMatrix", model.getMatrix()));
		modelRenderList.add(model);
	}

	public static void removeModel(Model model) {
		modelRenderList.remove(model);
	}

	public static void addDefaultObjects() {
		addScreen();
		addCrosshairs();
		addFPSMonitor();
		addCreeper();
		addSuzanne();
	}

	private static void addScreen() {
		guiRenderList.add(new Model(deferredStageScreenVertexArray, screenMaterial));
	}

	private static void addCrosshairs() {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(MeshGenerator.generateCrosshairs(null, 0.02f));
		vertexArray.create();
		final Model model = new Model(vertexArray, wireframeMaterial);
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
		final StringModel sandboxModel = new StringModel(glFactory, fontProgram, "SandboxPweryCusticRF0123456789,&: ", ubuntu.deriveFont(Font.PLAIN, 15), WINDOW_SIZE.getFloorX());
		final float aspect = 1 / ASPECT_RATIO;
		sandboxModel.setPosition(new Vector3(0.005, aspect / 2 + 0.315, -0.1));
		final String white = "#ffffffff", brown = "#ffC19953", green = "#ff00ff00", cyan = "#ff4fB5ff";
		sandboxModel.setString(brown + "Sandbox\n" + white + "Powered by " + green + "Caustic" + white + " & " + cyan + "React");
		guiRenderList.add(sandboxModel);
		final StringModel fpsModel = sandboxModel.getInstance();
		fpsModel.setPosition(new Vector3(0.005, aspect / 2 + 0.285, -0.1));
		fpsModel.setString("FPS: " + fpsMonitor.getFPS());
		guiRenderList.add(fpsModel);
		fpsMonitorModel = fpsModel;
	}

	private static void addCreeper() {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(loadOBJ(Sandbox.class.getResourceAsStream("/models/creeper.obj")));
		vertexArray.create();
		final Model mobModel = new Model(vertexArray, creeperMaterial);
		mobModel.setPosition(new Vector3(10, 10, 0));
		mobModel.setRotation(org.spout.math.imaginary.Quaternion.fromAngleDegAxis(-90, 0, 1, 0));
		addModel(mobModel);
		// Add a second mob, instanced from the first one
		movingMobModel = mobModel.getInstance();
		addModel(movingMobModel);
	}

	private static void addSuzanne() {
		final VertexArray vertexArray = glFactory.createVertexArray();
		vertexArray.setData(loadCollada(Sandbox.class.getResourceAsStream("/models/suzanne.dae")));
		vertexArray.create();
		final Model model = new Model(vertexArray, solidMaterial);
		model.setPosition(new Vector3(0, 10, -10));
		model.getUniforms().add(new ColorUniform("modelColor", sphereModelColor));
		addModel(model);
	}

	public static void startFPSMonitor() {
		fpsMonitor.start();
	}

	public static void render() {
		// UPDATE PER-FRAME UNIFORMS
		inverseViewMatrixUniform.set(modelCamera.getViewMatrix().invert());
		lightViewMatrixUniform.set(lightCamera.getViewMatrix());
		lightProjectionMatrixUniform.set(lightCamera.getProjectionMatrix());
		blurStrengthUniform.set((float) fpsMonitor.getFPS() / Sandbox.TARGET_FPS);
		// ANIMATE MOVING MOB
		final float time = (System.currentTimeMillis() % 1000) / 1000f;
		movingMobModel.setPosition(new Vector3(2 * TrigMath.sin(2 * (float) TrigMath.PI * time), 0, 0).add(-10, 10, 0));
		movingMobModel.setRotation(Quaternion.fromAngleDegAxis(time * 360, 1, 1, 1));
		// RENDER
		pipeline.run(context);
		// UPDATE PREVIOUS FRAME UNIFORMS
		setPreviousModelMatrices();
		previousViewMatrixUniform.set(modelCamera.getViewMatrix());
		previousProjectionMatrixUniform.set(modelCamera.getProjectionMatrix());
		// UPDATE FPS
		updateFPSMonitor();
	}

	private static void setPreviousModelMatrices() {
		for (Model model : modelRenderList) {
			model.getUniforms().getMatrix4("previousModelMatrix").set(model.getMatrix());
		}
	}

	private static void updateFPSMonitor() {
		fpsMonitor.update();
		fpsMonitorModel.setString("FPS: " + fpsMonitor.getFPS());
	}

	private static VertexData loadMesh(Vector3 sizes, TFloatList positions, TFloatList textureCoords, TFloatList normals, TIntList indices) {
		final VertexData vertexData = new VertexData();
		// POSITIONS
		final VertexAttribute positionAttribute = new VertexAttribute("positions", DataType.FLOAT, sizes.getFloorX());
		positionAttribute.setData(positions);
		vertexData.addAttribute(0, positionAttribute);
		// NORMALS
		final VertexAttribute normalAttribute;
		if (sizes.getZ() <= 0) {
			normalAttribute = new VertexAttribute("normals", DataType.FLOAT, 3);
			CausticUtil.generateNormals(positions, indices, normals);
		} else {
			normalAttribute = new VertexAttribute("normals", DataType.FLOAT, sizes.getFloorZ());
		}
		normalAttribute.setData(normals);
		vertexData.addAttribute(1, normalAttribute);
		// TEXTURE COORDS
		if (sizes.getY() > 0) {
			final VertexAttribute textureCoordAttribute = new VertexAttribute("textureCoords", DataType.FLOAT, sizes.getFloorY());
			textureCoordAttribute.setData(textureCoords);
			vertexData.addAttribute(2, textureCoordAttribute);
			// TANGENTS
			final VertexAttribute tangentAttribute = new VertexAttribute("tangents", DataType.FLOAT, 4);
			tangentAttribute.setData(CausticUtil.generateTangents(positions, normals, textureCoords, indices));
			vertexData.addAttribute(3, tangentAttribute);
		}
		// INDICES
		vertexData.getIndices().addAll(indices);
		return vertexData;
	}

	private static VertexData loadOBJ(InputStream file) {
		// LOAD
		final TFloatList positions = new TFloatArrayList();
		final TFloatList textureCoords = new TFloatArrayList();
		final TFloatList normals = new TFloatArrayList();
		final TIntList indices = new TIntArrayList();
		final Vector3 sizes = ObjFileLoader.load(file, positions, textureCoords, normals, indices);
		return loadMesh(sizes, positions, textureCoords, normals, indices);
	}

	private static VertexData loadCollada(InputStream in) {
		final TFloatList positions = new TFloatArrayList();
		final TFloatList textureCoords = new TFloatArrayList();
		final TFloatList normals = new TFloatArrayList();
		final TIntList indices = new TIntArrayList();
		return loadMesh(
				ColladaFileLoader.loadMesh(in, positions, textureCoords, normals, indices),
				positions, textureCoords, normals, indices
		);
	}

	public static void saveScreenshot() {
		final ByteBuffer buffer = context.readCurrentFrame(new Rectangle(Vector2.ZERO, WINDOW_SIZE), Format.RGB);
		final int width = context.getWindowWidth();
		final int height = context.getWindowHeight();
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		final byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final int srcIndex = (x + y * width) * 3;
				final int destIndex = (x + (height - y - 1) * width) * 3;
				data[destIndex + 2] = buffer.get(srcIndex);
				data[destIndex + 1] = buffer.get(srcIndex + 1);
				data[destIndex] = buffer.get(srcIndex + 2);
			}
		}
		try {
			ImageIO.write(image, "PNG", new File("screenshots" + File.separator + SCREENSHOT_DATE_FORMAT.format(Calendar.getInstance().getTime()) + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static class DoDeferredStageAction extends RenderModelsAction {
		private final FrameBuffer frameBuffer;

		private DoDeferredStageAction(FrameBuffer frameBuffer, VertexArray screen, Material material) {
			super(Arrays.asList(new Model(screen, material)));
			this.frameBuffer = frameBuffer;
		}

		@Override
		public void execute(Context context) {
			frameBuffer.bind();
			super.execute(context);
		}
	}
}
