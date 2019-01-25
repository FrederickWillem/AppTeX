var id_index = 1; //we start with 1, for the demo TeX code

function test() {
    document.getElementById("link").innerHTML="TEST";
}

function setTeX(tex) {
    id_index += 1;
    document.getElementById("link").innerHTML="Loaded Tex....";
    document.getElementById("math").innerHTML="$$" + tex + "$$";
    MathJax.Hub.Queue(['Typeset', MathJax.Hub]);
}

function save() {
    var canvas = document.getElementById('canvas');
    var ctx = canvas.getContext('2d');
    var glyphs = document.getElementById("MathJax_SVG_glyphs");

    var span = document.getElementById("MathJax-Element-" + id_index + "-Frame");
    var svg = span.firstChild;
    var width = svg.style.width;
    var height = svg.style.height;
    svg.style.width = width.substring(0, width.length-2)*4 + "ex";
    svg.style.height = height.substring(0, height.length-2)*4 + "ex";
    var svgString = '<' + '?xml version="1.0" encoding="UTF-8" standalone="no" ?' + '>\n';
    svgString += '<svg xmlns="http://www.w3.org/2000/svg"';
    for (var i = 0; i < svg.attributes.length; i++) svgString += ' ' + svg.attributes[i].name + '="' + svg.attributes[i].value + '"';
    svg.style.width = width;
    svg.style.height = height;
    svgString += '>\n';
    svgString += glyphs.outerHTML;
    svgString += '\n';
    svgString += svg.innerHTML;
    svgString += '\n</svg>';

    canvg('canvas', svgString);
    var url = canvas.toDataURL('image/png');
    var link  = document.getElementById("link")
    link.href = url;
    link.download = "image.png";
    link.click();
}