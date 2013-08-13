#version 330

const int MAX_KERNEL_SIZE = 128;

in vec2 textureUV;
noperspective in vec3 viewDirection;

layout(location = 0) out vec3 outputOcclusion;

uniform sampler2D normals;
uniform sampler2D depths;
uniform sampler2D noise;
uniform mat4 projectionMatrix;
uniform int kernelSize;
uniform vec3[MAX_KERNEL_SIZE] kernel;
uniform float radius;
uniform vec2 noiseScale;

float linearizeDepth(in float depth) {
    return projectionMatrix[3][2] / (depth + projectionMatrix[2][2]);
}

void main() {
    // Reconstruct the position of the fragment from the depth
    float depth = texture(depths, textureUV).r;
    depth = linearizeDepth(depth);
    vec3 origin = viewDirection * depth;

    // Get the fragment's normal
    vec3 normal = texture(normals, textureUV).xyz;

    // Construct a change of basis matrix to reorient our sample kernel along the object's normal.

    // Extract the random vector from the noise texture
    vec3 rvec = texture(noise, textureUV * noiseScale).xyz;

    // Calculate the tangent and bitangent using gram-shmidt.
    vec3 tangent = normalize(rvec - normal * dot(rvec, normal));
    vec3 bitangent = cross(normal, tangent);

    // Create the kernel basis matrix
    mat3 tbn = mat3(tangent, bitangent, normal);

    float occlusion = 0;

    float f = 0;

    for (int i = 0; i < kernelSize; i++) {
        // Get the sample position
        vec3 sample = tbn * kernel[i];
        sample = sample * radius + origin;

        // Project the sample
        vec4 offset = projectionMatrix * vec4(sample, 1);
        offset.xy /= offset.w;
        offset.xy = offset.xy * 0.5 + 0.5;

        // Get the sample depth
        float sampleDepth = linearizeDepth(texture(depths, offset.xy).r);

        // Range check and accumulate
        float rangeCheck = smoothstep(0, 1, radius / abs(origin.z - sampleDepth));
        occlusion += rangeCheck * step(sampleDepth, sample.z);

        f += rangeCheck;
    }

    occlusion = 1 - occlusion / kernelSize;

    f /= kernelSize;

    outputOcclusion = vec3(occlusion, occlusion, occlusion);
    //outputOcclusion = vec3(depth, depth, depth);
    //outputOcclusion = (normal + 1) / 2;
    //outputOcclusion = vec3(v.xy, 0);
}
