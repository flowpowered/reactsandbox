#version 120

const int MAX_KERNEL_SIZE = 128;

varying vec2 textureUV;
varying vec3 viewRay;

uniform sampler2D normals;
uniform sampler2D depths;
uniform sampler2D noise;
uniform mat4 projectionMatrix;
uniform int kernelSize;
uniform vec3[MAX_KERNEL_SIZE] kernel;
uniform float radius;
uniform vec2 noiseScale;
uniform float power;

float linearizeDepth(in float depth) {
    return projectionMatrix[3][2] / (depth + projectionMatrix[2][2]);
}

void main() {
    // Reconstruct the position of the fragment from the depth
    float depth = linearizeDepth(texture2D(depths, textureUV).r);
    vec3 origin = viewRay * depth;

    // Get the fragment's normal
    vec3 normal = texture2D(normals, textureUV).xyz;

    // Construct a change of basis matrix to reorient our sample kernel along the object's normal
    // Extract the random vector from the noise texture
    vec3 noiseVector = texture2D(noise, textureUV * noiseScale).xyz;

    // Calculate the tangent and bi-tangent using Gram-Schmidt
    vec3 tangent = normalize(noiseVector - normal * dot(noiseVector, normal));
    vec3 biTangent = cross(normal, tangent);

    // Create the kernel basis matrix
    mat3 tbn = mat3(tangent, biTangent, normal);

    float occlusion = 0;
    for (int i = 0; i < kernelSize; i++) {
        // Get the sample position
        vec3 sample = tbn * kernel[i];
        sample = sample * radius + origin;

        // Project the sample
        vec4 offset = projectionMatrix * vec4(sample, 1);
        offset.xy /= offset.w;
        offset.xy = offset.xy * 0.5 + 0.5;

        // Get the sample depth
        float sampleDepth = -linearizeDepth(texture2D(depths, offset.xy).r);

        // Range check and accumulate
        float rangeCheck = smoothstep(0, 1, radius / abs(origin.z - sampleDepth));
        occlusion += rangeCheck * step(sample.z, sampleDepth);
    }

    // Average and invert occlusion
    occlusion = pow(1 - occlusion / kernelSize, power);

    gl_FragColor = vec4(occlusion, occlusion, occlusion, 1);
}
