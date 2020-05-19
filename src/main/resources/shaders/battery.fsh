#version 330

#define PI 3.1415926538
#define WAVE_SIZE 0.05

in vec2 textureCoord;
out vec4 fragColor;

uniform vec3 color;
uniform float wavePosition;
uniform float level;

void main()
{
    if(textureCoord.x <= level)
        fragColor = vec4(color, 1.0);
    else if(sin(textureCoord.y*PI+wavePosition)*WAVE_SIZE+WAVE_SIZE+level > textureCoord.x)
        fragColor = vec4(color, 1.0);
    else
        fragColor = vec4(0.0);
}
