#version 120

varying vec3 modelPosition;
varying vec3 modelNormal;
varying vec3 viewDirection;

uniform vec4 modelColor;
uniform vec3 lightPosition;
uniform float lightAttenuation;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;

void main() {
    vec3 lightDifference = lightPosition - modelPosition;
    float lightDistance = length(lightDifference);
    vec3 lightDirection = lightDifference / lightDistance;
    float distanceIntensity = 1 / (1 + lightAttenuation * lightDistance);

    vec4 diffuse = modelColor * distanceIntensity * clamp(dot(modelNormal, lightDirection), 0, 1);
    vec4 specular = modelColor * distanceIntensity * pow(clamp(dot(reflect(-lightDirection, modelNormal), viewDirection), 0, 1), 2);
    vec4 ambient = modelColor;

    gl_FragColor = diffuse * diffuseIntensity + specular * specularIntensity + ambient * ambientIntensity;
}
