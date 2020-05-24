#version 330

in vec2 textureCoord;

out vec4 fragColor;

uniform sampler2D texImage;
uniform vec4 outlineColor;

void main()
{
    // calculate fragment's radius in circle
    vec2 circleCoord = textureCoord - vec2(0.5, 0.5);
    float r = sqrt(dot(circleCoord, circleCoord));

    // if outline is transparent, completly ignore it
    if(r < 0.475 || outlineColor.w == 0.0)
    {
        vec4 textureColor = texture(texImage, textureCoord);
        fragColor = textureColor;
    }
    else
    {
        fragColor = outlineColor;
    }
}
